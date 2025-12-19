CREATE KEYSPACE IF NOT EXISTS shortener
            WITH REPLICATION = {'class':'NetworkTopologyStrategy','dc1':3};