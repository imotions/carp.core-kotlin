import * as extend from "carp.core-kotlin-carp.studies.core"
import * as kotlinStdLib from "./kotlin"
import * as kotlinDateTime from "./kotlinx-datetime"
import * as kotlinSerialization from "./kotlinx-serialization"
import * as carpCommon from "./carp-common"


declare module "carp.core-kotlin-carp.studies.core"
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
        type Collection<T> = kotlinStdLib.kotlin.collections.Collection<T>
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
export * from "carp.core-kotlin-carp.studies.core"
