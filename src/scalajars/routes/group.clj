(ns scalajars.routes.group
  (:require [compojure.core :refer [GET POST defroutes]]
            [scalajars.web.group :as view]
            [scalajars.db :as db]
            [scalajars.auth :as auth]))

(defroutes routes
  (GET ["/groups/:groupname", :groupname #"[^/]+"] [groupname]
       (if-let [membernames (db/group-membernames groupname)]
         (auth/try-account
          (view/show-group account groupname membernames))))
  (POST ["/groups/:groupname", :groupname #"[^/]+"] [groupname username]
        (if-let [membernames (db/group-membernames groupname)]
          (auth/try-account
           (auth/require-authorization
            groupname
            (cond
             (some #{username} membernames)
             (view/show-group account groupname membernames
                              "They're already a member!")
             (db/find-user username)
             (do (db/add-member groupname username account)
                 (view/show-group account groupname
                                  (conj membernames username)))
             :else
             (view/show-group account groupname membernames
                              (str "No such user: "
                                   username))))))))
