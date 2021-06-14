import json
import os

import base64

import oisp

FRONTEND_URL = os.getenv(
    "OISP_FRONTEND_SERVICE") or "http://frontend:4001/v1/api"


def load_config_from_env(varname, seen_keys=None):
    """Read OISP config, which is an extended JSON format
    Values starting with @@ or %% are further ENV variables."""
    if seen_keys is None:
        seen_keys = []
    config = json.loads(os.environ[varname])
    for key, value in config.items():
        try:
            if value[:2] in ['@@', '%%']:
                assert key not in seen_keys, "Cyclic config"
                seen_keys.append(key)
                config[key] = load_config_from_env(value[2:], seen_keys[:])
        except TypeError:  # value not indexable = not string or unicode
            pass
    return config


config = {}
config["ruleEngineConfig"] = load_config_from_env("OISP_RULEENGINE_CONFIG")
config["kafkaConfig"] = load_config_from_env("OISP_KAFKA_CONFIG")
config["zookeeperConfig"] = load_config_from_env("OISP_ZOOKEEPER_CONFIG")


def get_tokens(users):
    """Given a list of dictionaries consisting of keys
    'user', 'password'; return a dictionary user->token."""
    tokens = {}
    for user_data in users:
        client = oisp.Client(FRONTEND_URL)
        client.auth(user_data["user"], user_data["password"])
        tokens[user_data["user"]] = client.get_user_token().value
    return tokens


def format_template(string, tokens=None, encode=None):
    if tokens is None:
        tokens = {}
    format_values = {"config": config,
                     "tokens": tokens}

    result = string.format(**format_values)
    if encode == "base64":
        result = base64.b64encode(result.encode("utf-8")).decode("utf-8")
    else:
        assert encode is None, f"Unknown encoding {encode}"
    return result
