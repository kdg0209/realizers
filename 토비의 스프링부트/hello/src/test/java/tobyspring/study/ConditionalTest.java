package tobyspring.study;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConditionalTest {

    @Test
    void conditionalA() {

        new ApplicationContextRunner().withUserConfiguration(Config1.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Config1.class);
                    assertThat(context).hasSingleBean(MyBean.class);
                });

        new ApplicationContextRunner().withUserConfiguration(Config2.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(Config2.class);
                    assertThat(context).doesNotHaveBean(MyBean.class);
                });
    }

    @Test
    void conditionalB() {

        // true
        AnnotationConfigApplicationContext ac1 = new AnnotationConfigApplicationContext();
        ac1.register(Config1.class);
        ac1.refresh();

        MyBean myBean1 = ac1.getBean(MyBean.class);
        assertThat(myBean1).isNotNull();

        // false

        AnnotationConfigApplicationContext ac2 = new AnnotationConfigApplicationContext();
        ac2.register(Config2.class);
        ac2.refresh();

        assertThatThrownBy(() -> ac2.getBean(MyBean.class))
                .isInstanceOf(BeansException.class);
    }

    @Configuration
    @BooleanConditional(value = true)
    static class Config1 {

        @Bean
        MyBean myBean() {
            return new MyBean();
        }
    }

    @Configuration
    @BooleanConditional(value = false)
    static class Config2 {

        @Bean
        MyBean myBean() {
            return new MyBean();
        }
    }

    static class MyBean {

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Conditional(BooleanCondition.class)
    @interface BooleanConditional {

        boolean value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Conditional(TrueCondition.class)
    @interface TrueConditional {

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Conditional(FalseCondition.class)
    @interface FalseConditional {

    }

    static class BooleanCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Map<String, Object> attributes = metadata.getAnnotationAttributes(BooleanConditional.class.getName());
            return (Boolean) attributes.get("value");
        }
    }

    static class TrueCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return true;
        }
    }

    static class FalseCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return false;
        }
    }
}
