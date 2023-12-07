(ns datascript.test.storage
  (:require
   [clojure.edn :as edn]
   [clojure.test :as t :refer [is are deftest testing]]
   [datascript.core :as d]
   [datascript.storage :as storage]))

(defrecord Storage [*disk *reads *writes *deletes]
  storage/IStorage
  (-store [_ addr+data-seq]
    (doseq [[addr data] addr+data-seq]
      (vswap! *disk assoc addr (pr-str data))
      (vswap! *writes conj addr)))

  (-restore [_ addr]
    (vswap! *reads conj addr)
    (-> @*disk (get addr) edn/read-string)))

(defn make-storage [& [opts]]
  (map->Storage
   {:*disk    (volatile! {})
    :*reads   (volatile! [])
    :*writes  (volatile! [])
    :*deletes (volatile! [])}))

(deftest test-basics
  (testing "empty db"
    (let [db      (d/empty-db)
          storage (make-storage)]
      (d/store db storage)
      (is (= 5 (count @(:*writes storage))))
      (let [db' (d/restore storage)]
        (is (= 2 (count @(:*reads storage))))   ;; read root + tail
        (is (= db db'))                         ;; read eavt
        (is (= 3 (count @(:*reads storage))))))))

(deftest test-conn
  (let [storage (make-storage)
        conn    (d/create-conn nil {:storage          storage
                                    :branching-factor 32
                                    :ref-type         :strong})]
    (is (= 5 (count @(:*writes storage)))) ;; initial store

    (d/transact! conn [[:db/add 1 :name "Ivan"]])
    (is (= 6 (count @(:*writes storage))))
    (is (= @#'storage/tail-addr (last @(:*writes storage))))))