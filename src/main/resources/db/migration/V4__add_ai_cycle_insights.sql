create table if not exists ai_cycle_insights (
  id bigserial primary key,
  cycle_a_id bigint not null,
  cycle_b_id bigint not null,
  model text not null,
  insight_text text not null,
  estimated_input_tokens integer not null,
  estimated_output_tokens integer not null,
  estimated_cost_usd numeric(12, 6) not null,
  created_at timestamptz not null default now()
);

create unique index if not exists ux_ai_cycle_insights_pair_model
  on ai_cycle_insights(cycle_a_id, cycle_b_id, model);
