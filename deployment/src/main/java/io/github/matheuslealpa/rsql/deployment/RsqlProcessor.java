package io.github.matheuslealpa.rsql.deployment;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.EntityManagerFactory;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import io.github.matheuslealpa.rsql.RsqlException;
import io.github.matheuslealpa.rsql.RsqlIgnore;
import io.github.matheuslealpa.rsql.runtime.IgnoredAttributes;
import io.github.matheuslealpa.rsql.runtime.RsqlRecorder;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;

class RsqlProcessor {

    private static final String FEATURE = "rsql";

    private static final DotName RSQL_IGNORE = DotName.createSimple(RsqlIgnore.class);

    /**
     * Named rather than referenced, so that the deployment module doesn't load a class that
     * depends on the optional Quarkus REST API.
     */
    private static final String EXCEPTION_MAPPER = "io.github.matheuslealpa.rsql.runtime.RsqlExceptionMapper";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Keeps the entity manager factories, which {@code Rsql} looks up programmatically to read
     * the metamodel, even when the application doesn't inject them.
     */
    @BuildStep
    UnremovableBeanBuildItem unremovableEntityManagerFactories() {
        return UnremovableBeanBuildItem.beanTypes(EntityManagerFactory.class);
    }

    /**
     * Collects the fields and getters annotated with {@link RsqlIgnore}, so that they are found
     * without reflection at runtime.
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void ignoredMembers(CombinedIndexBuildItem index, RsqlRecorder recorder) {
        Set<String> members = new HashSet<>();
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(RSQL_IGNORE)) {
            AnnotationTarget target = annotation.target();
            if (target.kind() == AnnotationTarget.Kind.FIELD) {
                FieldInfo field = target.asField();
                members.add(IgnoredAttributes.fieldKey(field.declaringClass().name().toString(), field.name()));
            } else if (target.kind() == AnnotationTarget.Kind.METHOD && target.asMethod().parametersCount() == 0) {
                MethodInfo method = target.asMethod();
                members.add(IgnoredAttributes.methodKey(method.declaringClass().name().toString(), method.name()));
            }
        }
        recorder.setIgnoredMembers(members);
    }

    /**
     * Forgets the entity metamodels cached by a previous start in dev mode.
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void clearEntityTypes(RsqlRecorder recorder) {
        recorder.clearEntityTypes();
    }

    /**
     * Reports invalid filters as {@code 400 Bad Request} when the application has Quarkus REST,
     * unless it prefers to map {@link RsqlException} itself.
     */
    @BuildStep
    void exceptionMapper(RsqlBuildTimeConfig config, Capabilities capabilities,
            BuildProducer<ExceptionMapperBuildItem> mappers) {
        if (config.mapExceptions() && capabilities.isPresent(Capability.REST)) {
            mappers.produce(new ExceptionMapperBuildItem.Builder(EXCEPTION_MAPPER, RsqlException.class.getName()).build());
        }
    }
}
