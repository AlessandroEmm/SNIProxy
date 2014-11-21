# SNI Proxy #

This Proxy enables transparent proxying of TLS 1.1< connections to endpoints.

Usage sbt "run 8080=file 9090=file2"

Where "file" consists of hostname-mappings in the json format .e.g 

	{ "www.google.com": "91.21.32.49" } 

If no mapping should take place then the file can point nowhere, and will be ignored.

## License ##

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
