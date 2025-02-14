/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.dataset.infra.aop;

import com.chutneytesting.campaign.infra.aop.CampaignIndexingAspect;
import com.chutneytesting.dataset.infra.index.DatasetIndexRepository;
import com.chutneytesting.server.core.domain.dataset.DataSet;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DatasetIndexingAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetIndexingAspect.class);
    private final DatasetIndexRepository datasetIndexRepository;

    public DatasetIndexingAspect(DatasetIndexRepository datasetIndexRepository) {
        this.datasetIndexRepository = datasetIndexRepository;
    }

    @AfterReturning(
        pointcut = "execution(* com.chutneytesting.dataset.infra.FileDatasetRepository.save(..)) && args(dataSet)",
        returning = "id",
        argNames = "dataSet,id")
    public void index(DataSet dataSet, String id) {
        try {
            datasetIndexRepository.save(DataSet.builder().fromDataSet(dataSet).withId(id).build());
        } catch (Exception e) {
            LOGGER.error("Error when indexing dataset: ", e);
        }
    }

    @After("execution(* com.chutneytesting.dataset.infra.FileDatasetRepository.removeById(..)) && args(id)")
    public void delete(String id) {
        try {
            datasetIndexRepository.delete(id);
        } catch (Exception e) {
            LOGGER.error("Error when deleting dataset index: ", e);
        }
    }
}
