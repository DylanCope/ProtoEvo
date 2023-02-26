package com.protoevo.utils.file;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.protoevo.physics.Particle;
import com.protoevo.env.Environment;

import java.io.IOException;

public class ParticleKeyDeserializer extends KeyDeserializer
{
    public static class IdGenerator extends ObjectIdGenerator<String> {
        private static final long serialVersionUID = 1L;
        private Class<?> scope;

        public IdGenerator() {
            this(Environment.class);
        }

        private IdGenerator(Class<?> scope) {
            this.scope = scope;
        }

        @Override
        public Class<?> getScope() {
            return scope;
        }

        @Override
        public boolean canUseFor(ObjectIdGenerator<?> gen) {
            return (gen.getClass() == getClass()) && (gen.getScope() == scope);
        }

        @Override
        public ObjectIdGenerator<String> forScope(Class<?> scope) {
            if (this.scope != scope)
                return new IdGenerator(scope);
            return this;
        }

        @Override
        public ObjectIdGenerator<String> newForSerialization(Object context) {
            return new IdGenerator(Environment.class);
        }

        @Override
        public IdKey key(Object key) {
            return new IdKey(getClass(), scope, key);
        }

        @Override
        public String generateId(Object forPojo) {
            if (!(forPojo instanceof Particle))  {
                return null;
            }
            return idFromHash(forPojo.hashCode());
        }

        public static String idFromHash(int hashCode) {
            return "Particle:" + hashCode;
        }
    }

    @Override
    public Object deserializeKey(final String key, final DeserializationContext ctxt) throws IOException, JsonProcessingException
    {
        int hashCode = Integer.parseInt(key.split("@")[1]);
        IdGenerator idGen = new IdGenerator(Environment.class);
        ObjectIdInfo idInfo = new ObjectIdInfo(
                new PropertyName("@id"),
                Environment.class,
                IdGenerator.class,
                null);
        ObjectIdResolver resolver = ctxt.objectIdResolverInstance(null, idInfo);
        ReadableObjectId roid = ctxt.findObjectId(
                IdGenerator.idFromHash(hashCode),
                idGen,
                resolver);

        return roid.resolve();
    }
}
