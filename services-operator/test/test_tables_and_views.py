from unittest import TestCase
import unittest
from bunch import Bunch
from mock import patch, MagicMock
import kopf
from kopf.testing import KopfRunner

import tables_and_views as target


"""
Mock functions
"""

global_message = ''

class Logger():
    def info(self, message):
        pass

    def debug(self, message):
        pass

    def error(self, message):
        pass

    def warning(self, message):
        global global_message
        global_message = message
        pass


def kopf_info(body, reason, message):
    pass



class TestDdlCreation(TestCase):
    create_kafka_ddl_used = False
    create_upsert_kafka_ddl_used = False

    def create_kafka_ddl(beamsqltable, logger):
        TestDdlCreation.create_kafka_ddl_used = True
        return


    @patch('tables_and_views.create_kafka_ddl',
        create_kafka_ddl)
    def test_create_ddl_from_beamsqltable(self):
        TestDdlCreation.create_kafka_ddl_used = False

        beamsqltable = Bunch()
        beamsqltable.metadata = Bunch()
        beamsqltable.metadata.name = "name"
        beamsqltable.metadata.namespace = "namespace"
        beamsqltable.spec = {
            "connector": "kafka"
        }

        target.create_ddl_from_beamsqltables(beamsqltable, Logger())
        self.assertEqual(TestDdlCreation.create_kafka_ddl_used, True)


    def create_upsert_kafka_ddl(beamsqltable, logger):
        TestDdlCreation.create_upsert_kafka_ddl_used = True
        return


    @patch('tables_and_views.create_upsert_kafka_ddl',
        create_upsert_kafka_ddl)
    def test_create_upsert_ddl_from_beamsqltable(self):
        TestDdlCreation.create_upsert_kafka_ddl_used = False

        beamsqltable = Bunch()
        beamsqltable.metadata = Bunch()
        beamsqltable.metadata.name = "name"
        beamsqltable.metadata.namespace = "namespace"
        beamsqltable.spec = {
            "connector": "upsert-kafka"
        }

        target.create_ddl_from_beamsqltables(beamsqltable, Logger())
        self.assertEqual(TestDdlCreation.create_upsert_kafka_ddl_used, True)

    def test_warning(self):
        global global_message
        global_message = ""

        beamsqltable = Bunch()
        beamsqltable.metadata = Bunch()
        beamsqltable.metadata.name = "name"
        beamsqltable.metadata.namespace = "namespace"
        beamsqltable.spec = {
            "connector": "wrong"
        }

        target.create_ddl_from_beamsqltables(beamsqltable, Logger())
        self.assertTrue(global_message.startswith("Beamsqltable name has not supported connector:"))


class TestcreateKafkaDdl(TestCase):
    def test_create_ddl_from_beamsqltable(self):
        global global_message
        global_message = ""
        beamsqltable = Bunch()
        beamsqltable.metadata = Bunch()
        beamsqltable.metadata.name = "name"
        beamsqltable.metadata.namespace = "namespace"
        beamsqltable.spec = {
            "connector": "kafka",
            "name": "name",
            "fields": [
                {"field1": "field1"},
                {"field2": "field2"}
            ],
            "value": {
                "format": "json"
            },
            "kafka": {
                "topic": "topic",
                "properties": {
                    "bootstrap.servers": "bootstrap.servers"
                }
            }
        }

        result = target.create_kafka_ddl(beamsqltable, Logger())
        self.assertEqual(result, "CREATE TABLE `name` (`field1` field1,`field2` field2) WITH "\
            "('connector' = 'kafka','format' = 'json', 'topic' = 'topic','properties.bootstrap.servers' = 'bootstrap.servers');")


    def test_create_upsert_ddl_from_beamsqltable(self):
        global global_message
        global_message = ""
        beamsqltable = Bunch()
        beamsqltable.metadata = Bunch()
        beamsqltable.metadata.name = "name"
        beamsqltable.metadata.namespace = "namespace"
        beamsqltable.spec = {
            "connector": "upsert-kafka",
            "name": "name",
            "fields": [
                {"field1": "field1"},
                {"field2": "field2"}
            ],
            "primaryKey": ["primaryKey"],
            "value": {
                "format": "json"
            },
            "kafka": {
                "topic": "topic",
                "key.format":  "json",
                "properties": {
                    "bootstrap.servers": "bootstrap.servers"
                }
            }
        }

        result = target.create_upsert_kafka_ddl(None, beamsqltable, Logger())
        self.assertEqual(result, "CREATE TABLE `name` (`field1` field1,`field2` field2, PRIMARY KEY (primaryKey) NOT ENFORCED) WITH "\
            "('connector' = 'upsert-kafka','value.format' = 'json', 'topic' = 'topic', 'key.format' = 'json',"\
            "'properties.bootstrap.servers' = 'bootstrap.servers');")

    def test_create_upsert_ddl_from_beamsqltable(self):
        beamsqlview = Bunch()
        beamsqlview.metadata = Bunch()
        beamsqlview.metadata.name = "name"
        beamsqlview.metadata.namespace = "namespace"
        beamsqlview.spec = {
            "name": "name",
            "sqlstatement": "sqlstatement"
        }

        result = target.create_view(beamsqlview)
        self.assertEqual(result, "CREATE VIEW `name` AS sqlstatement;")

if __name__ == '__main__':
    unittest.main()
