declare module "kotlin-kotlin-stdlib-js-ir"
{
    namespace $_$
    {
        interface Long
        {
            // toNumber
            x4(): number
        }
        function toLong( number: number ): Long

        interface Collection<T>
        {
            // contains
            g( value: T ): boolean

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
    }
}
