
create or replace function configuration.get_configuration
(
	_hostname varchar(100)
)
returns setof refcursor
as $$
declare
	_instance_id integer;
	log_instance refcursor;
	configuration_channel refcursor;
	configuration_channel_message_type refcursor;
begin

	insert into log.instance
	(
		instance_id,
		hostname,
		added_date,
		last_get_config_date
	)
	select
		coalesce(max(instance_id), 0) + 1 as instance_id,
		_hostname,
		now(),
		now()
	from log.instance
	on conflict (hostname) do 
	update
	set last_get_config_date = now()
	where instance.hostname = _hostname
	returning instance_id into _instance_id;

	------------------------------------------------------
	log_instance = 'log_instance';

	open log_instance for
	select _instance_id as instance_id;
	
	return next log_instance;
	
	------------------------------------------------------
	configuration_channel = 'configuration_channel';

	open configuration_channel for
	select 
		c.channel_id,
		c.channel_name,
		c.port_number,
		c.is_active,
		c.use_tls,
		c.sending_application,
		c.sending_facility,
		c.receiving_application,
		c.receiving_facility,
		c.port_number,
		c.notes
	from configuration.channel c;
	
	return next configuration_channel;
	
	------------------------------------------------------
	configuration_channel_message_type = 'configuration_channel_message_type';

	open configuration_channel_message_type for
	select
		t.channel_id,
		t.message_type,
		t.is_allowed		
	from configuration.channel_message_type t;
	
	return next configuration_channel_message_type;
	
end;
$$ language plpgsql;
