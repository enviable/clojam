(ns clojam.core.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]])
  (:import (java.net Socket)
    (java.io PrintWriter InputStreamReader BufferedReader)))

(def piano {:url "127.0.0.1" :port 4445})
(def piano-user {:user "admin" :pass "password"})

(def logs (list))

(declare conn-handler)

(defn connect [server]
  (let [socket (Socket. (:name server) (:port server))
      in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
      out (PrintWriter. (.getOutputStream socket))
      conn (ref {:in in :out out})]
    (doto (Thread. #(conn-handler conn)) (.start))
    conn))

(defn write [conn msg]
  (doto (:out @conn)
    (.println (str msg "\r"))
    (.flush)))

(defn conn-handler [conn]
  (while (nil? (:exit @conn))
    (let [msg (.readLine (:in @conn))]
      (println msg)

      (cond
        (re-find #"^ERROR :Closing Link:" msg)
        (dosync (alter conn merge {:exit true}))
        (re-find #"^PING" msg)
        (write conn (str "PONG " (re-find #":.*" msg)))))))

(defn login [conn user]
  (write conn (str "USER " (:user user) (:pass user))))


(defn command [conn cmd]
  (write conn cmd))


(defn playing-now []
  (def piano-connection (connect piano))
  (login piano-connection piano)
  (command piano-connection "STATUS"))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/playingnow" [] (playing-now))
  (route/not-found "Not Found"))


(def app
  (wrap-defaults app-routes site-defaults))




;/services/pandora/
