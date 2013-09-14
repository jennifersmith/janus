(defproject janus "0.0.2"
  :description "Consumer-driven contracts, verified both ways."
  :url "http://github.com/gga/janus"
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojure/data.json "0.1.1"]
                 [json-path "0.2.0"]
                 [clj-http "0.3.2"]
                 [org.clojure/core.logic "0.6.6"]
                 [org.clojure/tools.cli "0.2.1"]
                 [org.clojure/data.xml "0.0.3"]]
  :profiles {:dev {:dependencies [ [clj-http-fake "0.2.3"]
                                   [midje "1.6-beta1"]]
}}
  :main janus)
