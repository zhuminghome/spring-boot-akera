package com.ekrea.config;


import java.lang.reflect.Method;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * redis 缓存配置; 注意：RedisCacheConfig这里也可以不用继承
 * ：CachingConfigurerSupport，也就是直接一个普通的Class就好了； 这里主要我们之后要重新实现
 * key的生成策略，只要这里修改KeyGenerator，其它位置不用修改就生效了。
 * 普通使用普通类的方式的话，那么在使用@Cacheable的时候还需要指定KeyGenerator的名称;这样编码的时候比较麻烦。
 */
@Configuration
@EnableCaching // 启用缓存，这个注解很重要；
public class RedisConfig extends CachingConfigurerSupport {

	/**
     * 生成key的策略
     *
     * @return
     */
    @Bean
    public KeyGenerator keyGenerator() {
        return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                StringBuilder sb = new StringBuilder();
                sb.append(target.getClass().getName());
                sb.append(method.getName());
                for (Object obj : params) {
                    sb.append(obj.toString());
                }
                return sb.toString();
            }
        };
    }

	/**
     * 缓存管理器.
     * @param redisTemplate
     * @return
     */
    @Bean
    public CacheManager cacheManager(RedisTemplate<?,?> redisTemplate) {
    	RedisCacheManager cacheManager = new RedisCacheManager(redisTemplate);
    	return cacheManager;
    }

	/**
	 * redis模板操作类,类似于jdbcTemplate的一个类;
	 * 虽然CacheManager也能获取到Cache对象，但是操作起来没有那么灵活；
	 * 这里在扩展下：RedisTemplate这个类不见得很好操作，我们可以在进行扩展一个我们
	 * 自己的缓存类，比如：RedisStorage类;
	 * @param factory : 通过Spring进行注入，参数在application.properties进行配置；
	 * @return
	 * 
	 */
	@SuppressWarnings("rawtypes")
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(factory);
		/*设置redis序列化方式，默认使用JdkSerializationRedisSerializer二进制形式，不直观*/
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
       /* om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);*/
        om.configure(SerializationFeature.INDENT_OUTPUT, true);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        //设置value的序列化方式
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setKeySerializer(new GenericToStringSerializer<>(Object.class));
        redisTemplate.afterPropertiesSet();
        
        
		
		// key序列化方式;（不然会出现乱码;）,但是如果方法上有Long等非String类型的话，会报类型转换错误；
		// 所以在没有自己定义key生成策略的时候，以下这个代码建议不要这么写，可以不配置或者自己实现ObjectRedisSerializer
		// 或者JdkSerializationRedisSerializer序列化方式;	
		// RedisSerializer<String>redisSerializer = new StringRedisSerializer();//Long类型不可以会出现异常信息;
		// redisTemplate.setKeySerializer(redisSerializer);
		// redisTemplate.setHashKeySerializer(redisSerializer);

		return redisTemplate;
	}
}
