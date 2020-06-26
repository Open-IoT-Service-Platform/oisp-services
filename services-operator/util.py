import json
import os

import base64

example_config = ("""{{"application_name": "rule_engine_dashboard",
          "dashboard_strict_ssl": false,
          "dashboard_url": "http://{config[frontendUri]}"),
          "kafka_servers": "{config[kafkaConfig][uri]}",
          "kafka_zookeeper_quorum": "{config[zookeeperConfig][zkCluster]}" ,
          "kafka_observations_topic": "{config[kafkaConfig][topicsObservations]}",
          "kafka_rule_engine_topic": "{config[kafkaConfig][topicsRuleEngine]}",
          "kafka_heartbeat_topic": "{config[kafkaConfig][topicsHeartbeatName]}",
          "kafka_heartbeat_interval": "{config[kafkaConfig][topicsHeartbeatInterval]}",
          "hadoop_security_authentication": "{config[hbaseConfig"[hadoopProperties][hadoop.security.authentication]}",
          "hbase_table_prefix": "local",
          "token": token,
          "zookeeper_hbase_quorum": "{config[zookeeperConfig][zkCluster].split(":")[0]}"
          }}""")


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


def format_template(string, encode=None):
    result = string.format(**globals())
    if encode == "base64":
        result = base64.b64encode(result.encode("utf-8")).decode("utf-8")
    else:
        assert encode is None, f"Unknown encoding {encode}"
    return result
