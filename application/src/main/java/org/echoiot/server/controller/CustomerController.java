package org.echoiot.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.customer.TbCustomerService;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.PerResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomerController extends BaseController {

    private final TbCustomerService tbCustomerService;

    public static final String IS_PUBLIC = "isPublic";
    public static final String CUSTOMER_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', the server checks that the customer is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the user belongs to the customer.";

    @ApiOperation(value = "Get Customer (getCustomerById)",
            notes = "Get the Customer object based on the provided Customer Id. "
                    + CUSTOMER_SECURITY_CHECK + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}", method = RequestMethod.GET)
    @ResponseBody
    public Customer getCustomerById(
            @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(ControllerConstants.CUSTOMER_ID) String strCustomerId) throws EchoiotException {
        checkParameter(ControllerConstants.CUSTOMER_ID, strCustomerId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            Customer customer = checkCustomerId(customerId, Operation.READ);
            if (!customer.getAdditionalInfo().isNull()) {
                processDashboardIdFromAdditionalInfo((ObjectNode) customer.getAdditionalInfo(), ControllerConstants.HOME_DASHBOARD);
            }
            return customer;
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiOperation(value = "Get short Customer info (getShortCustomerInfoById)",
            notes = "Get the short customer object that contains only the title and 'isPublic' flag. "
                    + CUSTOMER_SECURITY_CHECK + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/shortInfo", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getShortCustomerInfoById(
            @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(ControllerConstants.CUSTOMER_ID) String strCustomerId) throws EchoiotException {
        checkParameter(ControllerConstants.CUSTOMER_ID, strCustomerId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            Customer customer = checkCustomerId(customerId, Operation.READ);
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode infoObject = objectMapper.createObjectNode();
            infoObject.put("title", customer.getTitle());
            infoObject.put(IS_PUBLIC, customer.isPublic());
            return infoObject;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Title (getCustomerTitleById)",
            notes = "Get the title of the customer. "
                    + CUSTOMER_SECURITY_CHECK + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/title", method = RequestMethod.GET, produces = "application/text")
    @ResponseBody
    public String getCustomerTitleById(
            @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(ControllerConstants.CUSTOMER_ID) String strCustomerId) throws EchoiotException {
        checkParameter(ControllerConstants.CUSTOMER_ID, strCustomerId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            Customer customer = checkCustomerId(customerId, Operation.READ);
            return customer.getTitle();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create or update Customer (saveCustomer)",
            notes = "Creates or Updates the Customer. When creating customer, platform generates Customer Id as " + ControllerConstants.UUID_WIKI_LINK +
                    "The newly created Customer Id will be present in the response. " +
                    "Specify existing Customer Id to update the Customer. " +
                    "Referencing non-existing Customer Id will cause 'Not Found' error." +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Customer entity. " +
                    ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer", method = RequestMethod.POST)
    @ResponseBody
    public Customer saveCustomer(@ApiParam(value = "A JSON value representing the customer.") @RequestBody Customer customer) throws Exception {
        customer.setTenantId(getTenantId());
        checkEntity(customer.getId(), customer, PerResource.CUSTOMER);
        return tbCustomerService.save(customer, getCurrentUser());
    }

    @ApiOperation(value = "Delete Customer (deleteCustomer)",
            notes = "Deletes the Customer and all customer Users. " +
                    "All assigned Dashboards, Assets, Devices, etc. will be unassigned but not deleted. " +
                    "Referencing non-existing Customer Id will cause an error." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteCustomer(@ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION)
                               @PathVariable(ControllerConstants.CUSTOMER_ID) String strCustomerId) throws EchoiotException {
        checkParameter(ControllerConstants.CUSTOMER_ID, strCustomerId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.DELETE);
        tbCustomerService.delete(customer, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Customers (getCustomers)",
            notes = "Returns a page of customers owned by tenant. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customers", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Customer> getCustomers(
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ControllerConstants.CUSTOMER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.CUSTOMER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(customerService.findCustomersByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Customer by Customer title (getTenantCustomer)",
            notes = "Get the Customer using Customer Title. " + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/customers", params = {"customerTitle"}, method = RequestMethod.GET)
    @ResponseBody
    public Customer getTenantCustomer(
            @ApiParam(value = "A string value representing the Customer title.")
            @RequestParam String customerTitle) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(customerService.findCustomerByTenantIdAndTitle(tenantId, customerTitle), "Customer with title [" + customerTitle + "] is not found");
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
