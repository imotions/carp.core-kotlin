import * as extend from "carp.core-kotlin-carp.protocols.core"
import * as kotlinStdLib from "./kotlin"
import * as kotlinDateTime from "./kotlinx-datetime"
import * as kotlinSerialization from "./kotlinx-serialization"


declare module "carp.core-kotlin-carp.protocols.core"
{
    // Declare missing types for which no imports were generated.
    namespace kotlin
    {
        type Long = kotlinStdLib.kotlin.Long
    }
    namespace kotlin.time
    {
        type Duration = kotlinStdLib.kotlin.time.Duration
    }
    namespace kotlin.collections
    {
        type List<T> = kotlinStdLib.kotlin.collections.List<T>
        type Set<T> = kotlinStdLib.kotlin.collections.Set<T>
        type Map<K, V> = kotlinStdLib.kotlin.collections.Map<K, V>
    }
    namespace kotlinx.datetime
    {
        type Instant = kotlinDateTime.kotlinx.datetime.Instant
    }
    namespace kotlinx.serialization.json
    {
        type Json = kotlinSerialization.kotlinx.serialization.json.Json
    }
}


// Export facade.
export * from "carp.core-kotlin-carp.protocols.core"