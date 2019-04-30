docker run^
 --rm^
 -p 9200:9200^
 -it^
 -v %cd%\..\build\elasticsearch:/usr/share/elasticsearch/plugins/flexible-synonyms^
 -v %cd%\elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml^
 elasticsearch:6.6.2