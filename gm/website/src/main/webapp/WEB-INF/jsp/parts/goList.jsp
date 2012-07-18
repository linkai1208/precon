<table id="go_table">
    <thead>
        <tr>
        	<th class="annotation"><label><spring:message code="go_tab.annotation_title"/></label></th>
        	<th class="pval" tooltip="<spring:message code="go_tab.q_val.tooltip"/>"><label><spring:message code="go_tab.q_val_title"/></label></th>
        	<th class="coverage" tooltip="<spring:message code="go_tab.coverage.tooltip"/>"><label><spring:message code="go_tab.coverage_title"/></label></th>
        </tr>
    </thead>
    <tbody>
        <tr class="query">
            <td class="annotation" value="<spring:message code="go_tab.query_genes.description"/>" ocid="-1"><spring:message code="go_tab.query_genes.name"/></td>
            <td class="pval" value="0" tooltip="<spring:message code="go_tab.q_val.tooltip"/>"><spring:message code="go_tab.query_genes.q_val"/></td>
            <td class="coverage" value="0" tooltip="<spring:message code="go_tab.coverage.tooltip"/>"><spring:message code="go_tab.query_genes.coverage"/></td>
        </tr>
        <c:forEach items="${results.resultOntologyCategories}" var="rOCat">
            <tr>
                <td class="annotation" name="${rOCat.ontologyCategory.description}" value="${rOCat.ontologyCategory.description}" ocid="${rOCat.ontologyCategory.id}">
                    ${rOCat.ontologyCategory.description}
                </td>
                <td class="pval" tooltip="<spring:message code="go_tab.q_val.tooltip"/>" value="${rOCat.qValue}">
                    <fmt:formatNumber value="${rOCat.qValue}" pattern="#.##E0" />
                </td>
                <td class="coverage" tooltip="<spring:message code="go_tab.coverage.tooltip"/>" value="${rOCat.numAnnotatedInSample}">
                	${rOCat.numAnnotatedInSample} / ${rOCat.numAnnotatedInTotal}
                </td>
            </tr>
        </c:forEach>
    </tbody>
</table>