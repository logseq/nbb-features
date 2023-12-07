(ns datascript.test.core
  "This is a minimal version of datascript.test.core that works with nbb tests"
  (:require [cljs.test :as t]
            [datascript.core :as d]
            [cognitect.transit :as transit]))

(defmethod t/assert-expr 'thrown-msg? [_menv msg form]
  (let [[_ match & body] form]
    `(try
       ~@body
       (t/do-report {:type :fail, :message ~msg, :expected '~form, :actual nil})
       (catch :default e#
         (let [m# (.-message e#)]
           (if (= ~match m#)
             (t/do-report {:type :pass, :message ~msg, :expected '~form, :actual e#})
             (t/do-report {:type :fail, :message ~msg, :expected '~form, :actual e#}))
           e#)))))

(defn all-datoms [db]
  (into #{} (map (juxt :e :a :v)) (d/datoms db :eavt)))

(def no-namespace-maps {:before #(alter-var-root #'*print-namespace-maps* (constantly false))})

(defn transit-write [o type]
  (transit/write (transit/writer type) o))

(defn transit-write-str [o]
  (transit-write o :json))

(defn transit-read [s type]
  (transit/read (transit/reader type) s))

(defn transit-read-str [s]
  (transit-read s :json))