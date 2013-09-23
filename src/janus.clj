(ns janus
  [:gen-class]
  [:require [clojure.tools.cli]
   [janus.dsl]
   [janus.verify]
   [janus.text-presentation]
   [janus.support]])

(defn unsafe-verify [service-defn]
  (let [service (janus.dsl/construct-domain service-defn)]
    (janus.verify/verify-service service {})))

(defn verify [service]
  (try
    (let [service-defn (read-string (slurp service))
          results [(unsafe-verify service-defn)]]
      [0 results])
    (catch java.io.FileNotFoundException e
      [1 (str "Could not find '" service "'")])
    (catch RuntimeException e
      (do
        (. e printStackTrace)
        [1 (str "Invalid service in '" service "'. Error: " e)]))))

(defn -main [& args]
  (do
    (janus.support/environment)
    (let [config (clojure.tools.cli/cli args
                                          ["-v" "--verify" "Services to verify"])
            service (:verify (nth config 0))
            [status message] (cond
                              service (verify service)
                              :else [0 ""])]
        (do (println message)
            (System/exit status)))))
