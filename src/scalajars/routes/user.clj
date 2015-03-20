(ns scalajars.routes.user
  (:require [compojure.core :refer [GET POST defroutes]]
            [scalajars.auth :as auth]
            [scalajars.web.user :as view]
            [scalajars.db :as db]))

(defn show [username]
  (if-let [user (db/find-user username)]
    (auth/try-account
     (view/show-user account user))))

(defroutes routes
  (GET "/profile" {:keys [flash]}
       (auth/with-account
         (view/profile-form account flash)))
  (POST "/profile" {:keys [params]}
        (auth/with-account
          (view/update-profile account params)))

  (GET "/register" _
       (view/register-form))

  (GET "/forgot-password" _
       (view/forgot-password-form))
  (POST "/forgot-password" {:keys [params]}
        (view/forgot-password params))

  (GET "/users/:username" [username]
       (show username))
  (GET "/:username" [username]
       (show username)))
