(ns aatree.core
  (:require [aatree.nodes :refer :all]
            [aatree.lazy-nodes :refer :all]
            [aatree.virtual-nodes :refer :all])
  (:import (aatree AAMap AAVector AASet)
           (aatree.nodes FlexVector INoded IFactory)
           (clojure.lang RT MapEntry)
           (java.io File)
           (java.nio ByteBuffer LongBuffer)
           (java.nio.file StandardOpenOption OpenOption)
           (java.nio.channels FileChannel)
           (java.util BitSet)))

(set! *warn-on-reflection* true)

(defn addn [^FlexVector vec ndx val]
  (.addNode vec ndx val))

(defn dropn [vec & args]
  (reduce (fn [^FlexVector v i] (.dropNode v i)) vec args))

(defn load-vector [buffer opts]
  ((:load-vector opts) buffer opts))

(defn load-sorted-map [buffer opts]
  ((:load-sorted-map opts) buffer opts))

(defn load-sorted-set [buffer opts]
  ((:load-sorted-set opts) buffer opts))

(defn byte-length [noded]
  (node-byte-length (get-inode noded) (get-opts noded)))

(defn put-aa [buffer aa]
  (node-write (get-inode aa) buffer (get-opts aa)))

(defn has-aafactories [opts] (:new-sorted-map opts))

(defn standard-opts
  ([] (standard-opts {}))
  ([opts]
   (-> opts
       (assoc :new-sorted-map
              (fn [o]
                (let [c (:comparator o)]
                  (if c
                    (sorted-map-by c)
                    (sorted-map)))))
       (assoc :new-vector
              (fn [o] []))
       (assoc :new-sorted-set
              (fn [o]
                (let [c (:comparator o)]
                  (if c
                    (sorted-set-by c)
                    (sorted-set))))))))

(defn basic-opts
  ([] (basic-opts {}))
  ([opts]
   (-> opts
       (assoc :new-sorted-map
              (fn [o]
                (if (:coparator opts)
                  (new AAMap emptyNode o)
                  (new AAMap
                       emptyNode
                       (assoc o :comparator RT/DEFAULT_COMPARATOR)))))
       (assoc :new-vector
              (fn [o] (new AAVector emptyNode o)))
       (assoc :new-sorted-set
              (fn [o]
                (let [mpl
                      (if (:coparator o)
                        (new AAMap emptyNode o)
                        (new AAMap emptyNode (assoc
                                               o
                                               :comparator
                                               RT/DEFAULT_COMPARATOR)))]
                  (new AASet mpl)))))))

(defn lazy-opts
  ([] (lazy-opts {}))
  ([opts]
   (-> opts
       (assoc :node-read lazy-read)
       (assoc :load-vector load-lazy-vector)
       (assoc :load-sorted-map load-lazy-sorted-map)
       (assoc :load-sorted-set load-lazy-sorted-set)
       (assoc :new-sorted-map
              (fn [r]
                (let [r (if (:comparator r)
                          r
                          (assoc r :comparator RT/DEFAULT_COMPARATOR))
                      r (if (:factory-registry r)
                          r
                          (assoc r :factory-registry default-factory-registry))
                      r (map-opts r)]
                  (new AAMap emptyLazyNode r))))
       (assoc :new-vector
              (fn [o]
                (if (:factory-registry o)
                  (new AAVector emptyLazyNode (vector-opts o))
                  (new AAVector
                       emptyLazyNode
                       (vector-opts (assoc o :factory-registry default-factory-registry))))))
       (assoc :new-sorted-set
              (fn [o]
                (let [r o
                      r (if (:comparator r)
                          r
                          (assoc r :comparator RT/DEFAULT_COMPARATOR))
                      r (if (:factory-registry r)
                          r
                          (assoc r :factory-registry default-factory-registry))
                      r (set-opts r)]
                  (new AASet
                       (new AAMap emptyLazyNode r))))))))

(defn virtual-opts
  ([] (virtual-opts {}))
  ([opts]
   (let [opts (if (:comparator opts)
                opts
                (assoc opts :comparator RT/DEFAULT_COMPARATOR))
         opts (if (:factory-registry opts)
                opts
                (assoc opts :factory-registry default-factory-registry))
         opts (-> opts
                  (assoc :find-dropped-blocks find-dropped-blocks)
                  (assoc :node-read virtual-read)
                  (assoc :as-reference virtual-as-reference)
                  (assoc :load-vector load-virtual-vector)
                  (assoc :load-sorted-map load-virtual-sorted-map)
                  (assoc :load-sorted-set load-virtual-sorted-set)
                  (assoc :new-sorted-map
                         (fn [o] (new AAMap emptyVirtualNode (map-opts o))))
                  (assoc :new-vector
                         (fn [o] (new AAVector emptyVirtualNode (vector-opts o))))
                  (assoc :new-sorted-set
                         (fn [o] (new AASet (new AAMap emptyVirtualNode (set-opts o))))))]
   opts)))

(defn new-sorted-map [opts]
  ((:new-sorted-map opts) opts))

(defn new-vector [opts]
  ((:new-vector opts) opts))

(defn new-sorted-set [opts]
  ((:new-sorted-set opts) opts))

(defn file-save [^ByteBuffer buffer ^File file]
  (let [^FileChannel fc (FileChannel/open (.toPath file)
                                          (into-array OpenOption
                                                      [StandardOpenOption/CREATE
                                                       StandardOpenOption/TRUNCATE_EXISTING
                                                       StandardOpenOption/WRITE]))]
    (try
      (.write fc buffer)
      (catch Exception e
        (.close fc)
        (throw e)))
    (.close fc)))

(defn ^ByteBuffer file-load [^File file]
  (let [^FileChannel fc (FileChannel/open (.toPath file)
                                          (into-array OpenOption
                                                      [StandardOpenOption/CREATE
                                                       StandardOpenOption/READ]))]
    (try
      (let [size (.size fc)
            bb (ByteBuffer/allocate size)]
        (.read fc bb)
        (.flip bb)
        bb)
      (finally
        (.close fc)))))

(defn db-close [opts] ((:db-close opts) opts))

(defn db-get-sorted-map [opts] ((:db-get-sorted-map opts) opts))

(defn db-transaction-count [opts] ((:db-transaction-count opts) opts))

(defn db-send [app-updater opts] ((:db-send opts) app-updater opts))

(defn db-update [app-updater opts] ((:db-update opts) app-updater opts))

(defn db-allocated [opts] ((:db-allocated opts) opts))

(defn db-allocate [opts] ((:db-allocate opts) opts))

(defn db-release-pending [opts] ((:db-release-pending opts) opts))

(defn db-release [block-position opts] ((:db-release opts) block-position opts))

(defn db-process-pending [age trans opts] ((:db-process-pending opts) age trans opts))

(register-factory
  default-factory-registry
  vector-context
  (reify IFactory
    (factoryId [this] (byte \v))                            ;;;;;;;;;;;;;;;;;;;;;;;;;;; v aavector in aavector
    (instanceClass [this] aatree.AAVector)
    (qualified [this t2 opts] this)
    (valueLength [this node opts]
      (let [^INoded v (.getT2 node opts)]
        (node-byte-length (get-inode v) (get-opts v))))
    (deserialize [this node bb opts]
      ((:load-vector opts) bb opts))
    (writeValue [this node buffer opts]
      (let [^INoded v (.getT2 node opts)]
        (node-write (get-inode v) buffer (get-opts v))))
    (valueNode [this node opts]
      (let [^INoded v (.getT2 node opts)]
        (get-inode v)))))

(register-factory
  default-factory-registry
  vector-context
  (reify IFactory
    (factoryId [this] (byte \m))                            ;;;;;;;;;;;;;;;;;;;;;;;;;;; m aamap in aavector
    (instanceClass [this] aatree.AAMap)
    (qualified [this t2 opts] this)
    (valueLength [this node opts]
      (let [^INoded m (.getT2 node opts)]
        (node-byte-length (get-inode m) (get-opts m))))
    (deserialize [this node bb opts]
      ((:load-sorted-map opts) bb opts))
    (writeValue [this node buffer opts]
      (let [^INoded v (.getT2 node opts)]
        (node-write (get-inode v) buffer (get-opts v))))
    (valueNode [this node opts]
      (let [^INoded v (.getT2 node opts)]
        (get-inode v)))))

(register-factory
  default-factory-registry
  vector-context
  (reify IFactory
    (factoryId [this] (byte \s))                            ;;;;;;;;;;;;;;;;;;;;;;;;;;; s aaset in aavector
    (instanceClass [this] aatree.AASet)
    (qualified [this t2 opts] this)
    (valueLength [this node opts]
      (let [^INoded m (.getT2 node opts)]
        (node-byte-length (get-inode m) (get-opts m))))
    (deserialize [this node bb opts]
      ((:load-sorted-set opts) bb opts))
    (writeValue [this node buffer opts]
      (let [^INoded s (.getT2 node opts)]
        (node-write (get-inode s) buffer (get-opts s))))
    (valueNode [this node opts]
      (let [^INoded v (.getT2 node opts)]
        (get-inode v)))))

(register-factory
  default-factory-registry
  map-context
  (reify IFactory
    (factoryId [this] (byte \V))                            ;;;;;;;;;;;;;;;;;;;;;;;;;;; V aavector in aamap
    (instanceClass [this] aatree.AAVector)
    (qualified [this t2 opts] this)
    (sval [this inode opts]
      (key-sval this inode opts))
    (valueLength [this node opts]
      (let [^MapEntry map-entry (.getT2 node opts)
            ^INoded v (.getValue map-entry)]
        (+ (default-valueLength this node opts)
           (node-byte-length (get-inode v) (get-opts v)))))
    (deserialize [this node bb opts]
      (let [k (deserialize-sval this node bb opts)
            v ((:load-vector opts) bb opts)]
        (MapEntry. k v)))
    (writeValue [this node buffer opts]
      (default-write-value this node buffer opts)
      (let [^MapEntry map-entry (.getT2 node opts)
            ^INoded v (.getValue map-entry)]
        (node-write (get-inode v) buffer (get-opts v))))
    (valueNode [this node opts]
      (let [^MapEntry map-entry (.getT2 node opts)
            ^INoded v (.getValue map-entry)]
        (get-inode v)))))

(register-factory
  default-factory-registry
  map-context
  (reify IFactory
    (factoryId [this] (byte \M))                            ;;;;;;;;;;;;;;;;;;;;;;;;;;; M aamap in aamap
    (instanceClass [this] aatree.AAMap)
    (qualified [this t2 opts] this)
    (sval [this inode opts]
      (key-sval this inode opts))
    (valueLength [this node opts]
      (let [^MapEntry map-entry (.getT2 node opts)
            ^INoded m (.getValue map-entry)]
        (+ (default-valueLength this node opts)
           (node-byte-length (get-inode m) (get-opts m)))))
    (deserialize [this node bb opts]
      (let [k (deserialize-sval this node bb opts)
            v ((:load-sorted-map opts) bb opts)]
        (MapEntry. k v)))
    (writeValue [this node buffer opts]
      (default-write-value this node buffer opts)
      (let [^MapEntry map-entry (.getT2 node opts)
            ^INoded m (.getValue map-entry)]
        (node-write (get-inode m) buffer (get-opts m))))
    (valueNode [this node opts]
      (let [^MapEntry map-entry (.getT2 node opts)
            ^INoded v (.getValue map-entry)]
        (get-inode v)))))

(register-factory
  default-factory-registry
  map-context
  (reify IFactory
    (factoryId [this] (byte \S))                            ;;;;;;;;;;;;;;;;;;;;;;;;;;; S aaset in aamap
    (instanceClass [this] aatree.AASet)
    (qualified [this t2 opts] this)
    (sval [this inode opts]
      (key-sval this inode opts))
    (valueLength [this node opts]
      (let [^MapEntry map-entry (.getT2 node opts)
            ^INoded s (.getValue map-entry)]
        (+ (default-valueLength this node opts)
           (node-byte-length (get-inode s) (get-opts s)))))
    (deserialize [this node bb opts]
      (let [k (deserialize-sval this node bb opts)
            v ((:load-sorted-set opts) bb opts)]
        (MapEntry. k v)))
    (writeValue [this node buffer opts]
      (default-write-value this node buffer opts)
      (let [^MapEntry map-entry (.getT2 node opts)
            ^INoded s (.getValue map-entry)]
        (node-write (get-inode s) buffer (get-opts s))))
    (valueNode [this node opts]
      (let [^MapEntry map-entry (.getT2 node opts)
            ^INoded v (.getValue map-entry)]
        (get-inode v)))))
