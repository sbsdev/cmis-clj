# cmis-clj

A quick hack to export documents from a content store via
[CMIS](https://en.wikipedia.org/wiki/Content_Management_Interoperability_Services).

## Usage

Start a REPL and use it from there.

``` shell
(with-open [writer (io/writer "export/ebooks.csv")]
  (write-ebook-numbers writer (extract-ebook-numbers user password)))

(extract-content user password)
```

You might want to adapt the default values that are hard coded at the
top of `core.clj`.

# References

- [Apache Chemistry CMIS Code Samples](https://chemistry.apache.org/docs/cmis-samples/index.html)
- [OpenCMIS Client API Developer's Guide](https://chemistry.apache.org/java/developing/guide.html)
- [CMIS Query Language](https://community.alfresco.com/docs/DOC-5898-cmis-query-language)
- [Apache Chemistry CMIS Workbench](https://chemistry.apache.org/java/developing/tools/dev-tools-workbench.html)

## License

Copyright © 2018 Swiss Library for the Blind, Visually Impaired and Print Disabled.

Distributed under the [GNU Affero General Public
License](http://www.gnu.org/licenses/agpl-3.0.html). See the file
LICENSE.

