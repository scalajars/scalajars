(ns scalajars.main
  (:require [scalajars.scp]
            [ring.adapter.jetty :refer [run-jetty]]
            [scalajars.web :refer [scalajars-app]]
            [scalajars.promote :as promote]
            [scalajars.config :refer [config configure]]
            [clojure.tools.nrepl.server :as nrepl])
  (:import com.martiansoftware.nailgun.NGServer
           java.net.InetAddress)
  (:gen-class))

(defn start-jetty []
  (when-let [port (:port config)]
    (println "scalajars-web: starting jetty on" (str "http://" (:bind config) ":" port))
    (run-jetty #'scalajars-app {:host (:bind config)
                            :port port
                            :join? false})))

(defn start-nailgun []
  (when-let [port (:nailgun-port config)]
    (println "scalajars-web: starting nailgun on" (str (:nailgun-bind config) ":" port))
    (.run (NGServer. (InetAddress/getByName (:nailgun-bind config)) port))))

(defn -main [& args]
  (alter-var-root #'*read-eval* (constantly false))
  (configure args)
  (start-jetty)
  (nrepl/start-server :port (:nrepl-port config) :bind "127.0.0.1")
  (start-nailgun))

;; (def server (run-jetty #'scalajars-app {:port 8080 :join? false}))
;; (.stop server)
