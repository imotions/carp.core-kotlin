import * as extend from "kotlinx-serialization-kotlinx-serialization-json-js-ir"


// Facade with better method names and type conversions for internal types.
export namespace kotlinx.serialization.json
{
    export interface Json
    {
        encodeToString( serializer: any, value: any ): string
        decodeFromString( serializer: any, string: string ): any
    }
    export namespace Json
    {
        export const Default: Json = extend.$_$.Default_getInstance()
    }
}


// Augment internal types to implement facade.
declare module "kotlinx-serialization-kotlinx-serialization-json-js-ir"
{
    namespace $_$
    {
        interface JsonImpl extends kotlinx.serialization.json.Json {}
        abstract class JsonImpl implements kotlinx.serialization.json.Json {}
    }
}


// Implement base interfaces in internal types.
extend.$_$.JsonImpl.prototype.encodeToString =
    function( serializer: any, value: any ): string
    {
        return this.t12( serializer, value );
    };
extend.$_$.JsonImpl.prototype.decodeFromString =
    function( serializer: any, string: string ): any
    {
        return this.u12( serializer, string );
    };


// Re-export augmented types.
export * from "kotlinx-serialization-kotlinx-serialization-json-js-ir"
