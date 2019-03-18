docker run \
    --rm \
    -p 9200:9200 \
    -it \
    -v `pwd`/../build/elasticsearch:/usr/share/elasticsearch/plugins/flexible-synonyms \
    -v `pwd`/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml \
    elasticsearch:6.6.1