# oisp-service-metric-aggregator

Service which aggregates ALL data from the metrics kafka channel.
Windows: 1m and 1h
Regular metrics are stored as <aid>.<cid> in Cassandra
The name of the aggreagated metrics is <aid>.<cid>.aggregator.{minute, hour} for instance aggregator for one hour is named
<aid>.<cid>.aggregator.hour
The windows start always at full hours or minutes, i.e. hours intervals of a day are [12am-1am), [1am-2am), [2am-3am), ..., [11pm-12am)

