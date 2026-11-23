-- Hirafis security hotfix: prevent any authenticated user from claiming
-- an arbitrary searching request. Apply in Supabase SQL Editor.
-- Safe to run more than once.

 drop policy if exists "Participants update requests" on public.service_requests;
 create policy "Participants update requests"
   on public.service_requests for update
   using (
     auth.uid()::text = client_id::text
     or auth.uid()::text = assigned_craftsman_id::text
     or public.is_admin()
     or exists (
       select 1 from public.service_offers o
       where o.request_id = service_requests.id
         and o.craftsman_id = auth.uid()
         and o.status in ('pending', 'countered', 'accepted')
     )
   )
   with check (
     auth.uid()::text = client_id::text
     or auth.uid()::text = assigned_craftsman_id::text
     or public.is_admin()
   );


create or replace function public.protect_service_request_mutations()
returns trigger
language plpgsql
security definer
set search_path = public
as $fn$
begin
  -- Service-role jobs and trusted administrators may perform managed changes.
  if auth.uid() is null or public.is_admin() then
    return new;
  end if;

  -- Core request identity and customer-provided details are immutable after creation.
  if new.id is distinct from old.id
     or new.client_id is distinct from old.client_id
     or new.client_name is distinct from old.client_name
     or new.category_id is distinct from old.category_id
     or new.category_name is distinct from old.category_name
     or new.title is distinct from old.title
     or new.description is distinct from old.description
     or new.address_label is distinct from old.address_label
     or new.lat is distinct from old.lat
     or new.lng is distinct from old.lng
     or new.payment_method is distinct from old.payment_method
     or new.is_urgent is distinct from old.is_urgent
     or new.photo_uri is distinct from old.photo_uri
     or new.created_at is distinct from old.created_at then
    raise exception 'Protected service request fields cannot be changed';
  end if;

  -- Request owner: may adjust price while searching, cancel, or accept a real offer.
  if auth.uid()::text = old.client_id::text then
    if old.status = 'searching' and new.status = 'searching' then
      if new.assigned_craftsman_id is distinct from old.assigned_craftsman_id
         or new.accepted_price is distinct from old.accepted_price
         or new.assigned_craftsman_name is distinct from old.assigned_craftsman_name
         or new.assigned_craftsman_phone is distinct from old.assigned_craftsman_phone
         or new.assigned_craftsman_rating is distinct from old.assigned_craftsman_rating then
        raise exception 'A searching request cannot be assigned without accepting an offer';
      end if;
      if new.proposed_price < 40 or new.proposed_price > 10000 then
        raise exception 'Proposed price is outside the allowed range';
      end if;
      return new;
    end if;

    if new.status = 'cancelled' and old.status in ('searching', 'accepted') then
      if new.assigned_craftsman_id is distinct from old.assigned_craftsman_id
         or new.accepted_price is distinct from old.accepted_price then
        raise exception 'Cancellation cannot change assignment or accepted price';
      end if;
      return new;
    end if;

    if old.status = 'searching' and new.status = 'accepted'
       and new.assigned_craftsman_id is not null
       and new.accepted_price is not null
       and exists (
         select 1 from public.service_offers o
         where o.request_id = old.id
           and o.craftsman_id = new.assigned_craftsman_id
           and o.status in ('pending', 'countered', 'accepted')
       ) then
      return new;
    end if;

    raise exception 'Invalid customer request state transition';
  end if;

  -- Assigned craftsman: may update job state and live coordinates only.
  if auth.uid()::text = old.assigned_craftsman_id::text then
    if new.proposed_price is distinct from old.proposed_price
       or new.accepted_price is distinct from old.accepted_price
       or new.assigned_craftsman_id is distinct from old.assigned_craftsman_id
       or new.assigned_craftsman_name is distinct from old.assigned_craftsman_name
       or new.assigned_craftsman_phone is distinct from old.assigned_craftsman_phone
       or new.assigned_craftsman_rating is distinct from old.assigned_craftsman_rating then
      raise exception 'Craftsman cannot change customer pricing or assignment';
    end if;

    if new.status = old.status then
      return new;
    end if;
    if (old.status = 'accepted' and new.status in ('en_route', 'cancelled'))
       or (old.status = 'en_route' and new.status in ('arrived', 'cancelled'))
       or (old.status = 'arrived' and new.status in ('in_progress', 'cancelled'))
       or (old.status = 'in_progress' and new.status in ('completed', 'cancelled')) then
      return new;
    end if;
    raise exception 'Invalid craftsman request state transition';
  end if;

  -- An unassigned craftsman can accept only after submitting a live offer.
  if old.status = 'searching'
     and new.status = 'accepted'
     and new.assigned_craftsman_id::text = auth.uid()::text
     and new.accepted_price is not null
     and exists (
       select 1 from public.service_offers o
       where o.request_id = old.id
         and o.craftsman_id = auth.uid()
         and o.status in ('pending', 'countered', 'accepted')
     ) then
    return new;
  end if;

  raise exception 'Unauthorized request state mutation';
end;
$fn$;

drop trigger if exists trg_service_requests_tamper_proof on public.service_requests;
create trigger trg_service_requests_tamper_proof
  before update on public.service_requests
  for each row execute function public.protect_service_request_mutations();

-- Reviews are accepted only from the customer after a completed job, once per request.
drop policy if exists "Clients insert reviews" on public.reviews;
create policy "Clients insert reviews"
  on public.reviews for insert
  with check (
    auth.uid()::text = client_id::text
    and exists (
      select 1 from public.service_requests r
      where r.id::text = request_id::text
        and r.client_id::text = auth.uid()::text
        and r.assigned_craftsman_id::text = craftsman_id::text
        and r.status = 'completed'
    )
    and not exists (
      select 1 from public.reviews existing_review
      where existing_review.request_id = reviews.request_id
    )
  );

-- Prevent forged offers and edits to offer ownership/identity fields.
drop policy if exists "Craftsmen insert offers" on public.service_offers;
create policy "Craftsmen insert offers"
  on public.service_offers for insert
  with check (
    auth.uid()::text = craftsman_id::text
    and exists (
      select 1 from public.service_requests r
      where r.id = request_id
        and r.client_id = client_id
        and r.status = 'searching'
        and r.assigned_craftsman_id is null
    )
  );

create or replace function public.protect_service_offer_mutations()
returns trigger
language plpgsql
security definer
set search_path = public
as $fn$
declare
  request_owner uuid;
  request_status text;
  request_assignee uuid;
begin
  if auth.uid() is null or public.is_admin() then
    return new;
  end if;

  if new.id is distinct from old.id
     or new.request_id is distinct from old.request_id
     or new.client_id is distinct from old.client_id
     or new.craftsman_id is distinct from old.craftsman_id
     or new.craftsman_name is distinct from old.craftsman_name
     or new.craftsman_phone is distinct from old.craftsman_phone
     or new.craftsman_rating is distinct from old.craftsman_rating
     or new.completed_jobs is distinct from old.completed_jobs
     or new.specialty is distinct from old.specialty
     or new.eta_minutes is distinct from old.eta_minutes
     or new.distance_km is distinct from old.distance_km
     or new.created_at is distinct from old.created_at then
    raise exception 'Protected service offer fields cannot be changed';
  end if;

  select r.client_id, r.status, r.assigned_craftsman_id
    into request_owner, request_status, request_assignee
  from public.service_requests r where r.id = old.request_id;

  if request_status <> 'searching' or request_assignee is not null then
    raise exception 'Offers are locked after a request is assigned or closed';
  end if;

  if auth.uid()::text = request_owner::text then
    if new.status not in ('accepted', 'declined', 'countered') then
      raise exception 'Invalid customer offer action';
    end if;
    if new.status <> 'countered' and (
      new.offered_price is distinct from old.offered_price
      or new.note is distinct from old.note
      or new.expires_at_millis is distinct from old.expires_at_millis
    ) then
      raise exception 'Only a counter-offer may change price or note';
    end if;
    if new.offered_price < 40 or new.offered_price > 10000 then
      raise exception 'Offer price is outside the allowed range';
    end if;
    return new;
  end if;

  if auth.uid()::text = old.craftsman_id::text then
    if new.status not in ('pending', 'countered', 'declined', 'expired') then
      raise exception 'Invalid craftsman offer action';
    end if;
    if new.offered_price < 40 or new.offered_price > 10000 then
      raise exception 'Offer price is outside the allowed range';
    end if;
    return new;
  end if;

  raise exception 'Unauthorized service offer mutation';
end;
$fn$;

drop trigger if exists trg_service_offers_tamper_proof on public.service_offers;
create trigger trg_service_offers_tamper_proof
  before update on public.service_offers
  for each row execute function public.protect_service_offer_mutations();


-- Validate numeric boundaries on INSERT as well as UPDATE. NOT VALID keeps
-- legacy rows untouched while enforcing the rules for every new/changed row.
do $constraints$
begin
  if not exists (select 1 from pg_constraint where conname = 'profiles_experience_range') then
    alter table public.profiles add constraint profiles_experience_range
      check (experience_years between 0 and 60) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_base_price_range') then
    alter table public.profiles add constraint profiles_base_price_range
      check (base_price_dh between 40 and 5000) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_rating_range') then
    alter table public.profiles add constraint profiles_rating_range
      check (rating between 1 and 5) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_lat_range') then
    alter table public.profiles add constraint profiles_lat_range
      check (lat between -90 and 90) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_lng_range') then
    alter table public.profiles add constraint profiles_lng_range
      check (lng between -180 and 180) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'service_requests_lat_range') then
    alter table public.service_requests add constraint service_requests_lat_range
      check (lat between -90 and 90) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'service_requests_lng_range') then
    alter table public.service_requests add constraint service_requests_lng_range
      check (lng between -180 and 180) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'service_requests_price_range') then
    alter table public.service_requests add constraint service_requests_price_range
      check (proposed_price between 40 and 10000) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'service_requests_accepted_price_range') then
    alter table public.service_requests add constraint service_requests_accepted_price_range
      check (accepted_price is null or accepted_price between 40 and 10000) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'service_offers_price_range') then
    alter table public.service_offers add constraint service_offers_price_range
      check (offered_price between 40 and 10000) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'service_offers_eta_range') then
    alter table public.service_offers add constraint service_offers_eta_range
      check (eta_minutes between 1 and 240) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'service_requests_craftsman_lat_range') then
    alter table public.service_requests add constraint service_requests_craftsman_lat_range
      check (craftsman_lat is null or craftsman_lat between -90 and 90) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'service_requests_craftsman_lng_range') then
    alter table public.service_requests add constraint service_requests_craftsman_lng_range
      check (craftsman_lng is null or craftsman_lng between -180 and 180) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'service_offers_distance_nonnegative') then
    alter table public.service_offers add constraint service_offers_distance_nonnegative
      check (distance_km >= 0) not valid;
  end if;
end;
$constraints$;
