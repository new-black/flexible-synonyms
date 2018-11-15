docker run \
    --rm \
    -p 9200:9200 \
    -v `pwd`/../build/elasticsearch:/usr/share/elasticsearch/plugins/flexible-synonyms \
    -v `pwd`/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml \
    elasticsearch:5.6.4