declare module "kotlin-kotlin-stdlib-js-ir"
{
    namespace $_$
    {
        interface Long
        {
            // toNumber
            i5(): number
        }
        function toLong_0( number: number ): Long

        class Pair<K, V>
        {
            constructor( first: K, second: V )

            // first
            a3_1: K

            // second
            b3_1: V
        }

        interface Collection<T>
        {
            // contains
            d1( value: T ): boolean

            // size
            f(): number

            toArray(): Array<T>
        }

        interface List<T> extends Collection<T> {}
        interface EmptyList<T> extends List<T> {}
        interface AbstractMutableList<T> extends List<T> {}
        function listOf<T>( elements: T[] ): List<T>

        interface Set<T> extends Collection<T> {}
        interface EmptySet<T> extends Set<T> {}
        interface HashSet<T> extends Set<T> {}
        function setOf<T>( elements: T[] ): Set<T>

        interface Map<K, V>
        {
            // get
            e2( key: K ): V

            // keys
            f2(): Set<K>

            // values
            g2(): Collection<V>
        }
        interface HashMap<K, V> extends Map<K, V> {}
        function mapOf<K, V>( pairs: Pair<K, V>[] ): Map<K, V>

        interface Duration extends Long {}
        interface DurationCompanion
        {
            // parseIsoString
            j6(): Duration

            // ZERO
            g6_1: Duration

            // INFINITE
            h6_1: Duration
        }
        function Companion_getInstance_6(): DurationCompanion
        function _Duration___get_inWholeMilliseconds__impl__msfiry(duration: Duration): Long
        function _Duration___get_inWholeMicroseconds__impl__8oe8vv(duration: Duration): Long
    }
}