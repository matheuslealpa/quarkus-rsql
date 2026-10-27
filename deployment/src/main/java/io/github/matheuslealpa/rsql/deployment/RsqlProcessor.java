package io.github.matheuslealpa.rsql.deployment;

import jakarta.persistence.EntityManagerFactory;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class RsqlProcessor {

    private static final String FEATURE = "rsql";

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
}
