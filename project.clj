(defproject cmis-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.apache.chemistry.opencmis/chemistry-opencmis-client-impl "1.1.0"]
                 [sanitize-filename "0.1.0"]
                 [org.clojure/data.xml "0.2.0-alpha5"]
                 [org.clojure/data.zip "0.1.2"]
                 [org.clojure/data.csv "0.1.4"]]
  :main ^:skip-aot cmis-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
