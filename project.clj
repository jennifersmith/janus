(defproject janus "0.0.2"
  :description "Consumer-driven contracts, verified both ways."
  :url "http://github.com/gga/janus"
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojure/data.json "0.1.1"]
                 [json-path "0.2.0"]
                 [clj-http "0.3.2"]
                 [org.clojure/core.logic "0.6.6"]
                 [org.clojure/tools.cli "0.2.1"]
                 [org.clojure/data.xml "0.0.3"]
                 [ring/ring-core "1.2.0"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [org.mortbay.jetty/jetty "6.1.14"]
                 [ring-serve "0.1.2"]
                 [midje "1.6-beta1"]]
  :profiles {:dev {:dependencies [ [clj-http-fake "0.2.3"]
                                   [ring/ring-devel "1.1.0"]
                                   [ring/ring-json "0.2.0"]]
}}
  :main janus)
