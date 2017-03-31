INSERT INTO config
(app_id, config_id, config_data)
VALUES
('eds-ui', 'keycloak', '{
  "realm": "endeavour",
  "realm-public-key": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA7GdjckqAZgjxp/o7717ei5RgkW3mtG3W+LfmlboBt20NQ/Jz6yb00Xoe9dBCLsqiiompePWuBNxGdwUNHzJcng7hpTvsi7Zp8PtTJDts/EinroKEv+Gac2VB1k8aLneDOtU6FYdi7uQ4vVU4xJ4D4s1ubG0VQXqUnSUvwwRN5UDdGYLrV2KueajgsNJ3mML4aJ2rLDyUF5uvKQV1UbZAwvCUo0tIeUYoN6PMkpaUrBagWeLhNhrNU9HsiDbMUjVttDRgMlgCtYvu4GapI+0cVecAUWfg0MdTCYuUJwUtTZoatf3d2bietsS+cYPFfs9eCIm1/7GLZWwv6qFDN1a4ewIDAQAB",
  "auth-server-url": "https://devauth.endeavourhealth.net/auth",
  "ssl-required": "external",
  "resource": "eds-ui",
  "public-client": true
}' );

INSERT INTO config
(app_id, config_id, config_data)
VALUES
('eds-ui', 'keycloak_proxy', '{
 "user" : "eds-ui",
 "pass" : "bd285adbc36842d7a27088e93c36c13e29ed69fa63a6",
 "url" : "https://devauth.endeavourhealth.net/auth"
}');

INSERT INTO config
(app_id, config_id, config_data)
VALUES
('eds-ui', 'application', '{ "appUrl" : "http://localhost:8080" }');


/*INSERT INTO config
(app_id, config_id, config_data)
VALUES
('eds-ui', 'rePostMessageToExchangeConfig', '{
	"username" : "guest",
	"password" : "guest",
	"nodes" : ["localhost:5672"],
	"exchange" : "EdsInbound",
	"routingHeader" : "SenderLocalIdentifier"
}');*/

INSERT INTO config
(app_id, config_id, config_data)
VALUES
('eds-ui', 'logbackDb','{
   "url" : "jdbc:postgresql://localhost:5432/logback",
   "username" : "postgres",
   "password" : ""
}');


INSERT INTO config
(app_id, config_id, config_data)
VALUES
('eds-ui', 'OrganisationManagerDB','{
   "url" : "jdbc:mysql://URL_OF_SERVER/OrganisationManager",
   "username" : "USERNAME",
   "password" : "PASSWORD"
}');