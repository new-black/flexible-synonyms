# Elasticsearch plugin ![Travis](https://travis-ci.com/new-black/flexible-synonyms.svg?branch=master)

## For dynamically reloading synonyms

Currently supports specifying an endpoint which the plugin should poll every 15
seconds to retrieve new synonyms.

## Usage

Create an index with the `flexible_synonym` filter enabled.

```json
{
        "analysis" : {
            "analyzer" : {
                "synonym" : {
                    "tokenizer" : "whitespace",
                    "filter" : ["remote_synonym"]
               }
            },
            "filter" : {
                "remote_synonym" : {
                    "type" : "flexible_synonym",
                    "synonyms_uri" : "https://my-url-to-poll.com"
                }
            }
        }
}
``` 

And imagine the endpoint has this data:

```text
med, medicine, medical
```

Then start using the index like this:

`POST /my_index/analyze`

```json
{
  "analyzer": "synonym",
  "text": "med"
}
```

And you'll get a response with the synonyms:

```json
{
  "tokens" : [
    {
      "token" : "medical",
      "start_offset" : 0,
      "end_offset" : 3,
      "type" : "SYNONYM",
      "position" : 0
    },
    {
      "token" : "medicine",
      "start_offset" : 0,
      "end_offset" : 3,
      "type" : "SYNONYM",
      "position" : 0
    },
    {
      "token" : "med",
      "start_offset" : 0,
      "end_offset" : 3,
      "type" : "word",
      "position" : 0
    }
  ]
}
```

## Settings

- `synonyms_uri` location of the resource to poll for the set of available synonyms.
- `synonyms_format` format of the given resource, can be either solr or wordnet. Defaults to solr.
