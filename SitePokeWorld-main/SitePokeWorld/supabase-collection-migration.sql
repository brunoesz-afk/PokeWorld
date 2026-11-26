-- Backup da coleção inteira do jogo (time + defesa da base + caixa).
-- A tabela pokemon_party só aceita 6 lugares, por isso a caixa e a defesa ficam aqui.
-- Rode no SQL Editor do Supabase (uma vez).
create table if not exists public.pokemon_collection (
  user_id uuid not null references auth.users(id) on delete cascade,
  slot integer not null check (slot >= 1),
  pokemon_name text not null check (char_length(btrim(pokemon_name)) between 1 and 60),
  pokemon_level integer check (pokemon_level is null or pokemon_level between 1 and 999),
  shiny boolean not null default false,
  rarity text not null default 'COMMON',
  gender integer not null default 0,
  boost integer not null default 0,
  location text not null default 'box' check (location in ('party', 'guard', 'box')),
  primary key (user_id, slot)
);

grant select, insert, update, delete on public.pokemon_collection to authenticated;

alter table public.pokemon_collection enable row level security;

drop policy if exists "Users manage their own Pokemon collection" on public.pokemon_collection;
create policy "Users manage their own Pokemon collection"
  on public.pokemon_collection
  for all
  to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);
