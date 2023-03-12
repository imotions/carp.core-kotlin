import * as kotlinStdLib from "kotlin-kotlin-stdlib-js-ir"


// Facade with better method names and type conversions for internal types.
export namespace kotlin
{
    export interface Long
    {
        toNumber(): number
    }
    export const toLong: (number: number) => Long = kotlinStdLib.$_$.toLong_0
    export class Pair<K, V>
    {
        constructor( first: K, second: V ) {
            let kotlinPair = new kotlinStdLib.$_$.Pair( first, second );
            kotlinPair.first = kotlinPair.a3_1;
            kotlinPair.second = kotlinPair.b3_1;
            return kotlinPair;
        }
        get first(): K { return this.first; }
        get second(): V { return this.second; }
    }
}
export namespace kotlin.collections
{
    export interface Collection<T>
    {
        contains( value: T ): boolean
        size(): number
        toArray(): Array<T>
    }
    export interface List<T> extends Collection<T> {}
    export interface Set<T> extends Collection<T> {}
    export interface Map<K, V>
    {
        get( key: K ): V
        keys: Set<K>
        values: Collection<V>
    }
    export const listOf: <T>(array: T[]) => List<T> = kotlinStdLib.$_$.listOf
    export const setOf: <T>(array: T[]) => Set<T> = kotlinStdLib.$_$.setOf
    export const mapOf =
        function<K, V>( pairs: kotlin.Pair<K, V>[] ): Map<K, V>
        {
            return kotlinStdLib.$_$.mapOf( pairs as any )
        }
}
export namespace kotlin.time
{
    export interface Duration
    {
        get inWholeMilliseconds(): number
        get inWholeMicroseconds(): number
    }
    export namespace Duration
    {
        export const Companion: any = kotlinStdLib.$_$.Companion_getInstance_6()
        export const parseIsoString: (isoDuration: string) => Duration = Companion.j6
        export const ZERO: Duration = Companion.g6_1
        export const INFINITE: Duration = Companion.h6_1
    }
}


// Augment internal types to implement facade.
declare module "kotlin-kotlin-stdlib-js-ir"
{
    namespace $_$
    {
        abstract class Long implements kotlin.Long
        {
            toNumber(): number
            inWholeMilliseconds(): number
            inWholeMicroseconds(): number
        }
        interface Pair<K, V> extends kotlin.Pair<K, V>
        {
            first: K
            second: V
        }
        interface Collection<T> extends kotlin.collections.Collection<T> {}
        abstract class EmptyList<T> implements kotlin.collections.List<T> {}
        abstract class AbstractMutableList<T> implements kotlin.collections.List<T> {}
        interface Set<T> extends kotlin.collections.Set<T> {}
        abstract class EmptySet<T> implements kotlin.collections.Set<T> {}
        abstract class HashSet<T> implements kotlin.collections.Set<T> {}
        interface Map<K, V> extends kotlin.collections.Map<K, V> {}
        abstract class HashMap<K, V> implements kotlin.collections.Map<K, V>
        {
            get( key: K ): V
            keys: kotlin.collections.Set<K>
            values: kotlin.collections.Collection<V>
        }
    }
}


// Implement base interfaces in internal types.
kotlinStdLib.$_$.Long.prototype.toNumber = function(): number { return this.i5(); };
Object.defineProperty( kotlinStdLib.$_$.Long.prototype, "inWholeMilliseconds", {
    get: function inWholeMilliseconds()
    {
        return kotlinStdLib.$_$._Duration___get_inWholeMilliseconds__impl__msfiry(this).toNumber();
    }
} );
Object.defineProperty( kotlinStdLib.$_$.Long.prototype, "inWholeMicroseconds", {
    get: function inWholeMicroseconds()
    {
        return kotlinStdLib.$_$._Duration___get_inWholeMicroseconds__impl__8oe8vv(this).toNumber();
    }
} );
kotlinStdLib.$_$.EmptyList.prototype.contains = function<T>( value: T ): boolean { return false; }
kotlinStdLib.$_$.EmptyList.prototype.size = function<T>(): number { return 0; }
kotlinStdLib.$_$.EmptyList.prototype.toArray = function<T>(): T[] { return []; }
kotlinStdLib.$_$.AbstractMutableList.prototype.contains = function<T>( value: T ): boolean { return this.d1( value ); }
kotlinStdLib.$_$.AbstractMutableList.prototype.size = function<T>(): number { return this.f(); }
kotlinStdLib.$_$.EmptySet.prototype.contains = function<T>( value: T ): boolean { return false; }
kotlinStdLib.$_$.EmptySet.prototype.size = function<T>(): number { return 0; }
kotlinStdLib.$_$.EmptySet.prototype.toArray = function<T>(): T[] { return []; }
kotlinStdLib.$_$.HashSet.prototype.contains = function<T>( value: T ): boolean { return this.d1( value ); }
kotlinStdLib.$_$.HashSet.prototype.size = function<T>(): number { return this.f(); }
kotlinStdLib.$_$.HashMap.prototype.get = function<K, V>( key: K ): V { return this.e2( key ); }
Object.defineProperty( kotlinStdLib.$_$.HashMap.prototype, "keys", {
    get: function keys() { return this.f2(); }
} );
Object.defineProperty( kotlinStdLib.$_$.HashMap.prototype, "values", {
    get: function values() { return this.g2(); }
} );


// Re-export augmented types.
export * from "kotlin-kotlin-stdlib-js-ir";