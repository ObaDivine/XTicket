package com.ngxgroup.xpolicy;

import com.google.gson.Gson;
import com.ngxgroup.xpolicy.model.AppRoles;
import com.ngxgroup.xpolicy.model.Company;
import com.ngxgroup.xpolicy.model.Department;
import com.ngxgroup.xpolicy.model.Division;
import com.ngxgroup.xpolicy.model.GroupRoles;
import com.ngxgroup.xpolicy.model.PolicyType;
import com.ngxgroup.xpolicy.model.RoleGroups;
import com.ngxgroup.xpolicy.repository.XPolicyRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.stereotype.Component;

//@Component
public class ServletInitializer extends SpringBootServletInitializer implements ApplicationRunner {

    @Autowired
    XPolicyRepository xpolicyRepository;
    @Autowired
    Gson gson;
    @Value("${xpolicy.ngx.dc.group}")
    private String ngxGroupDC;
    @Value("${xpolicy.ngx.dc.regulation}")
    private String ngxRegulationDC;
    @Value("${xpolicy.ngx.dc.realestate}")
    private String ngxRealEstateDC;
    @Value("${xpolicy.ngx.dc.limited}")
    private String ngxLimitedDC;

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(XPolicyApplication.class);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Company ngxGroupCoy = null;
        Company ngxRegCoy = null;
        Company ngxRealEstateCoy = null;
        Company ngxLimitedCoy = null;
        List<Company> companies = xpolicyRepository.getCompanies();
        if (companies == null) {
            //Create NGX Group
            Company ngxCompanyRecord = xpolicyRepository.getCompanyUsingCode("NGX");
            if (ngxCompanyRecord == null) {
                Company ngxGroup = new Company();
                ngxGroup.setCompanyCode("NGX");
                ngxGroup.setCompanyHead("Mr Oscar Onyema");
                ngxGroup.setCompanyName("Nigerian Exchange Group");
                ngxGroup.setCreatedAt(LocalDateTime.now());
                ngxGroup.setDomainController(ngxGroupDC);
                ngxGroupCoy = xpolicyRepository.createCompany(ngxGroup);
            }

            //Create NGX Regulation
            Company ngxRegRecord = xpolicyRepository.getCompanyUsingCode("NREG");
            if (ngxRegRecord == null) {
                Company ngxReg = new Company();
                ngxReg.setCompanyCode("NREG");
                ngxReg.setCompanyHead("Mrs Tinuade Awe");
                ngxReg.setCompanyName("NGX Regulation Limited");
                ngxReg.setCreatedAt(LocalDateTime.now());
                ngxReg.setDomainController(ngxRegulationDC);
                ngxRegCoy = xpolicyRepository.createCompany(ngxReg);
            }

            //Create NGX Real Estate
            Company ngxRealEstateRecord = xpolicyRepository.getCompanyUsingCode("NREL");
            if (ngxRealEstateRecord == null) {
                Company ngxRealEstate = new Company();
                ngxRealEstate.setCompanyCode("NREL");
                ngxRealEstate.setCompanyHead("Mr Igbeka Gabby");
                ngxRealEstate.setCompanyName("NGX Real Estate Limited");
                ngxRealEstate.setCreatedAt(LocalDateTime.now());
                ngxRealEstate.setDomainController(ngxRealEstateDC);
                ngxRealEstateCoy = xpolicyRepository.createCompany(ngxRealEstate);
            }

            //Create NGX Limited
            Company ngxLimitedRecord = xpolicyRepository.getCompanyUsingCode("NGXL");
            if (ngxLimitedRecord == null) {
                Company ngxLimited = new Company();
                ngxLimited.setCompanyCode("NGXL");
                ngxLimited.setCompanyHead("Mr Temi Popoola");
                ngxLimited.setCompanyName("Nigerian Exchange Limited");
                ngxLimited.setCreatedAt(LocalDateTime.now());
                ngxLimited.setDomainController(ngxLimitedDC);
                ngxLimitedCoy = xpolicyRepository.createCompany(ngxLimited);
            }

        }

        //Check the divisions
        Division groupCEODiv = null;
        List<Division> ngxGroupDivisions = xpolicyRepository.getDivisionUsingCompany(ngxGroupCoy);
        if (ngxGroupDivisions == null) {
            Division officeofGCEPRecord = xpolicyRepository.getDivisionUsingCode("OGCEO");
            if (officeofGCEPRecord == null) {
                Division newDiv = new Division();
                newDiv.setCompany(ngxGroupCoy);
                newDiv.setCreatedAt(LocalDateTime.now());
                newDiv.setDivisionCode("OGCEO");
                newDiv.setDivisionHead("Mr Oscar Onyema");
                newDiv.setDivisionName("Office of GCEO");
                groupCEODiv = xpolicyRepository.createDivision(newDiv);
            }
        }

        Division regulationDiv = null;
        List<Division> ngxRegDivisions = xpolicyRepository.getDivisionUsingCompany(ngxRegCoy);
        if (ngxRegDivisions == null) {
            Division officeofRegulationRecord = xpolicyRepository.getDivisionUsingCode("OCEOREG");
            if (officeofRegulationRecord == null) {
                Division newDiv = new Division();
                newDiv.setCompany(ngxRegCoy);
                newDiv.setCreatedAt(LocalDateTime.now());
                newDiv.setDivisionCode("OCEOREG");
                newDiv.setDivisionHead("Ms Tinuade Awe");
                newDiv.setDivisionName("Office of CEO");
                regulationDiv = xpolicyRepository.createDivision(newDiv);
            }
        }

        Division realEstateDiv = null;
        List<Division> ngxRealEstateDivisions = xpolicyRepository.getDivisionUsingCompany(ngxRealEstateCoy);
        if (ngxRealEstateDivisions == null) {
            Division officeofGCEPRecord = xpolicyRepository.getDivisionUsingCode("OCEOREL");
            if (officeofGCEPRecord == null) {
                Division newDiv = new Division();
                newDiv.setCompany(ngxRealEstateCoy);
                newDiv.setCreatedAt(LocalDateTime.now());
                newDiv.setDivisionCode("OCEOREL");
                newDiv.setDivisionHead("Mr Igbeka Gabby");
                newDiv.setDivisionName("Office of CEO");
                realEstateDiv = xpolicyRepository.createDivision(newDiv);
            }
        }

        Division limitedCEODiv = null;
        Division capitalMarketDiv = null;
        Division digitalTechDiv = null;
        Division businessSupportDiv = null;
        List<Division> ngxLimitedDivisions = xpolicyRepository.getDivisionUsingCompany(ngxLimitedCoy);
        if (ngxLimitedDivisions == null) {
            Division officeofCEORecord = xpolicyRepository.getDivisionUsingCode("OCEO");
            if (officeofCEORecord == null) {
                Division newDiv = new Division();
                newDiv.setCompany(ngxLimitedCoy);
                newDiv.setCreatedAt(LocalDateTime.now());
                newDiv.setDivisionCode("OCEO");
                newDiv.setDivisionHead("Mr Temi Popoola");
                newDiv.setDivisionName("Office of CEO");
                limitedCEODiv = xpolicyRepository.createDivision(newDiv);
            }

            Division capitalMarketRecord = xpolicyRepository.getDivisionUsingCode("CMKT");
            if (capitalMarketRecord == null) {
                Division newDiv = new Division();
                newDiv.setCompany(ngxLimitedCoy);
                newDiv.setCreatedAt(LocalDateTime.now());
                newDiv.setDivisionCode("CMKT");
                newDiv.setDivisionHead("Mr Jude Chiemeka");
                newDiv.setDivisionName("Capital Market Service");
                capitalMarketDiv = xpolicyRepository.createDivision(newDiv);
            }

            Division digTechRecord = xpolicyRepository.getDivisionUsingCode("DTECH");
            if (digTechRecord == null) {
                Division newDiv = new Division();
                newDiv.setCompany(ngxLimitedCoy);
                newDiv.setCreatedAt(LocalDateTime.now());
                newDiv.setDivisionCode("DTECH");
                newDiv.setDivisionHead("Dr Olufemi Oyenuga");
                newDiv.setDivisionName("Digital & Technology Service");
                digitalTechDiv = xpolicyRepository.createDivision(newDiv);
            }

            Division businessSupportRecord = xpolicyRepository.getDivisionUsingCode("BSUP");
            if (businessSupportRecord == null) {
                Division newDiv = new Division();
                newDiv.setCompany(ngxLimitedCoy);
                newDiv.setCreatedAt(LocalDateTime.now());
                newDiv.setDivisionCode("BSUP");
                newDiv.setDivisionHead("Mrs Irene Robinson-Anyanwale");
                newDiv.setDivisionName("Business Support Service");
                businessSupportDiv = xpolicyRepository.createDivision(newDiv);
            }
        }

        //Check departments under each division
        List<Department> groupDivDepartments = xpolicyRepository.getDepartmentUsingDivision(groupCEODiv);
        if (groupDivDepartments == null) {
            Department groupInternaAudit = xpolicyRepository.getDepartmentUsingCode("GIAUD");
            if (groupInternaAudit == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("GIAUD");
                newDept.setDepartmentHead("Mr Bernard Ahanaonu");
                newDept.setDepartmentName("Group Internal Audit");
                newDept.setDivision(groupCEODiv);
                xpolicyRepository.createDepartment(newDept);
            }

            Department groupFinance = xpolicyRepository.getDepartmentUsingCode("GFINCON");
            if (groupFinance == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("GFINCON");
                newDept.setDepartmentHead("Mr Cyril Eigbobo");
                newDept.setDepartmentName("Group Finance");
                newDept.setDivision(groupCEODiv);
                xpolicyRepository.createDepartment(newDept);
            }

            Department groupStrategy = xpolicyRepository.getDepartmentUsingCode("GSTRG");
            if (groupStrategy == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("GSTRG");
                newDept.setDepartmentHead("Mr Onuntuel Okon");
                newDept.setDepartmentName("Strategy");
                newDept.setDivision(groupCEODiv);
                xpolicyRepository.createDepartment(newDept);
            }

            Department groupSecretariat = xpolicyRepository.getDepartmentUsingCode("GCOMSEC");
            if (groupSecretariat == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("GCOMSEC");
                newDept.setDepartmentHead("Mr Adebayo Akolade");
                newDept.setDepartmentName("Company Secretariat & Compliance");
                newDept.setDivision(groupCEODiv);
                xpolicyRepository.createDepartment(newDept);
            }

            Department groupInvestment = xpolicyRepository.getDepartmentUsingCode("GIVEST");
            if (groupInvestment == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("GIVEST");
                newDept.setDepartmentHead("Mr Idugboe Tony");
                newDept.setDepartmentName("Group Investment");
                newDept.setDivision(groupCEODiv);
                xpolicyRepository.createDepartment(newDept);
            }
        }

        List<Department> regulationDivDepartments = xpolicyRepository.getDepartmentUsingDivision(regulationDiv);
        if (regulationDivDepartments == null) {
            Department rulesAdjudication = xpolicyRepository.getDepartmentUsingCode("RULADJ");
            if (rulesAdjudication == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("RULADJ");
                newDept.setDepartmentHead("Mr Adenugba Oluwatoyin");
                newDept.setDepartmentName("Rules and Adjudication");
                newDept.setDivision(regulationDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department listingRegulation = xpolicyRepository.getDepartmentUsingCode("LSTREG");
            if (listingRegulation == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("LSTREG");
                newDept.setDepartmentHead("Mr Iwenekhal Godstime");
                newDept.setDepartmentName("Listings Regulation");
                newDept.setDivision(regulationDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department groupInvestment = xpolicyRepository.getDepartmentUsingCode("GIVEST");
            if (groupInvestment == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("GIVEST");
                newDept.setDepartmentHead("Mr Babalola Abimbola");
                newDept.setDepartmentName("Market Surveilance");
                newDept.setDivision(regulationDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department brokerDealer = xpolicyRepository.getDepartmentUsingCode("BRJDEL");
            if (brokerDealer == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("BRJDEL");
                newDept.setDepartmentHead("Mr Shobanjo Olufemi");
                newDept.setDepartmentName("Broker Delaer Regulation");
                newDept.setDivision(regulationDiv);
                xpolicyRepository.createDepartment(newDept);
            }
        }

        List<Department> realEstateDivDepartments = xpolicyRepository.getDepartmentUsingDivision(realEstateDiv);
        if (realEstateDivDepartments == null) {
            Department reception = xpolicyRepository.getDepartmentUsingCode("RCPDSKS");
            if (reception == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("RCPDSKS");
                newDept.setDepartmentHead("");
                newDept.setDepartmentName("Reception Desks");
                newDept.setDivision(realEstateDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department otherOffices = xpolicyRepository.getDepartmentUsingCode("OTHOFF");
            if (otherOffices == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("OTHOFF");
                newDept.setDepartmentHead("");
                newDept.setDepartmentName("Other Offices");
                newDept.setDivision(realEstateDiv);
                xpolicyRepository.createDepartment(newDept);
            }
        }

        List<Department> limitedCEODivDepartments = xpolicyRepository.getDepartmentUsingDivision(limitedCEODiv);
        if (limitedCEODivDepartments == null) {
            Department humanResources = xpolicyRepository.getDepartmentUsingCode("HMRS");
            if (humanResources == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("HMRS");
                newDept.setDepartmentHead("Mr Adebayo Ademola");
                newDept.setDepartmentName("Human Resources");
                newDept.setDivision(limitedCEODiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department finance = xpolicyRepository.getDepartmentUsingCode("FINCON");
            if (finance == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("FINCON");
                newDept.setDepartmentHead("Mr Opatade Adebayo");
                newDept.setDepartmentName("Finance");
                newDept.setDivision(limitedCEODiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department govtRelation = xpolicyRepository.getDepartmentUsingCode("GOVTREL");
            if (govtRelation == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("GOVTREL");
                newDept.setDepartmentHead("Mr Nabegu Abdullahi");
                newDept.setDepartmentName("Government Relations");
                newDept.setDivision(limitedCEODiv);
                xpolicyRepository.createDepartment(newDept);
            }
        }

        List<Department> digitalTechDivDepartments = xpolicyRepository.getDepartmentUsingDivision(digitalTechDiv);
        if (digitalTechDivDepartments == null) {
            Department digitalInnovation = xpolicyRepository.getDepartmentUsingCode("DDI");
            if (digitalInnovation == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("DDI");
                newDept.setDepartmentHead("Mr Afeez Ramoni");
                newDept.setDepartmentName("Data and Digital Innovation");
                newDept.setDivision(digitalTechDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department programmeManagement = xpolicyRepository.getDepartmentUsingCode("ENTPROGMGT");
            if (programmeManagement == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("ENTPROGMGT");
                newDept.setDepartmentHead("Ms Afolashade Idowu");
                newDept.setDepartmentName("Enterprise Programme Management");
                newDept.setDivision(digitalTechDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department techSupport = xpolicyRepository.getDepartmentUsingCode("TECHSUPP");
            if (techSupport == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("TECHSUPP");
                newDept.setDepartmentHead("Mr Tolulope Ajayi");
                newDept.setDepartmentName("Technology Support and Infrastructure");
                newDept.setDivision(digitalTechDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department infoSecurity = xpolicyRepository.getDepartmentUsingCode("INFOSEC");
            if (infoSecurity == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("INFOSEC");
                newDept.setDepartmentHead("Mrs Saad Maryam");
                newDept.setDepartmentName("Information Security");
                newDept.setDivision(digitalTechDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department techSales = xpolicyRepository.getDepartmentUsingCode("TECHSALE");
            if (techSales == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("TECHSALE");
                newDept.setDepartmentHead("Ms Irona Oputa");
                newDept.setDepartmentName("Technology Sales");
                newDept.setDivision(digitalTechDiv);
                xpolicyRepository.createDepartment(newDept);
            }
        }

        List<Department> capitalMarketDivDepartments = xpolicyRepository.getDepartmentUsingDivision(capitalMarketDiv);
        if (capitalMarketDivDepartments == null) {
            Department primaryKarket = xpolicyRepository.getDepartmentUsingCode("PRIMKT");
            if (primaryKarket == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("PRIMKT");
                newDept.setDepartmentHead("Mr Ibeziako Tony");
                newDept.setDepartmentName("Primary Markets");
                newDept.setDivision(capitalMarketDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department secondaryMarket = xpolicyRepository.getDepartmentUsingCode("SECMKT");
            if (secondaryMarket == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("SECMKT");
                newDept.setDepartmentHead("Mr Alimi Kazeem");
                newDept.setDepartmentName("Secondary Markets");
                newDept.setDivision(capitalMarketDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department stateEnterprise = xpolicyRepository.getDepartmentUsingCode("STOENT");
            if (stateEnterprise == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("STOENT");
                newDept.setDepartmentHead("Mr Owobi Blessing");
                newDept.setDepartmentName("State Owned Enterprises");
                newDept.setDivision(capitalMarketDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department xacademy = xpolicyRepository.getDepartmentUsingCode("XACAD");
            if (xacademy == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("XACAD");
                newDept.setDepartmentHead("Mrs Obi Ogechi");
                newDept.setDepartmentName("X Academy");
                newDept.setDivision(capitalMarketDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department productDevelopment = xpolicyRepository.getDepartmentUsingCode("PRODDEV");
            if (productDevelopment == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("PRODDEV");
                newDept.setDepartmentHead("Mrs Chukwueke-Okolo Chidinma");
                newDept.setDepartmentName("Product Development");
                newDept.setDivision(capitalMarketDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department marketOperations = xpolicyRepository.getDepartmentUsingCode("MKTOPS");
            if (marketOperations == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("MKTOPS");
                newDept.setDepartmentHead("Mr Ohaeri Kenneth");
                newDept.setDepartmentName("Market Operations");
                newDept.setDivision(capitalMarketDiv);
                xpolicyRepository.createDepartment(newDept);
            }

        }

        List<Department> businessSupportDivDepartments = xpolicyRepository.getDepartmentUsingDivision(businessSupportDiv);
        if (businessSupportDivDepartments == null) {
            Department adminService = xpolicyRepository.getDepartmentUsingCode("ADMSERV");
            if (adminService == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("ADMSERV");
                newDept.setDepartmentHead("Mr Ahime Ejemai");
                newDept.setDepartmentName("Administration Service");
                newDept.setDivision(businessSupportDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department procurement = xpolicyRepository.getDepartmentUsingCode("PROC");
            if (procurement == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("PROC");
                newDept.setDepartmentHead("Mr Ikhuruwefe Precious");
                newDept.setDepartmentName("Procurement");
                newDept.setDivision(businessSupportDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department security = xpolicyRepository.getDepartmentUsingCode("SECURE");
            if (security == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("SECURE");
                newDept.setDepartmentHead("Mr Olufemi Ifedayo");
                newDept.setDepartmentName("Security");
                newDept.setDivision(businessSupportDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department legalService = xpolicyRepository.getDepartmentUsingCode("LEGSERV");
            if (legalService == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("LEGSERV");
                newDept.setDepartmentHead("Mr Oghenekevwe Okpobia");
                newDept.setDepartmentName("Legal Service");
                newDept.setDivision(businessSupportDiv);
                xpolicyRepository.createDepartment(newDept);
            }
            Department riskAndControl = xpolicyRepository.getDepartmentUsingCode("RSKCTRL");
            if (riskAndControl == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("RSKCTRL");
                newDept.setDepartmentHead("Mr Akinola Akintayo");
                newDept.setDepartmentName("Risk and Control");
                newDept.setDivision(businessSupportDiv);
                xpolicyRepository.createDepartment(newDept);
            }

            Department corporateComms = xpolicyRepository.getDepartmentUsingCode("CORPCOMM");
            if (corporateComms == null) {
                Department newDept = new Department();
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setDepartmentCode("CORPCOMM");
                newDept.setDepartmentHead("Mr Akpolo Clifford");
                newDept.setDepartmentName("Market and Corporate Communication");
                newDept.setDivision(businessSupportDiv);
                xpolicyRepository.createDepartment(newDept);
            }
        }

        //Add Policy Type
        List<PolicyType> policyTypes = xpolicyRepository.getPolicyTypes();
        if (policyTypes == null) {
            PolicyType policies = xpolicyRepository.getPolicyTypeUsingCode("POL");
            if (policies == null) {
                PolicyType newType = new PolicyType();
                newType.setCreatedAt(LocalDateTime.now());
                newType.setPolicyTypeCode("POL");
                newType.setPolicyTypeName("Policies");
                xpolicyRepository.createPolicyType(newType);
            }
            PolicyType sop = xpolicyRepository.getPolicyTypeUsingCode("SOP");
            if (sop == null) {
                PolicyType newType = new PolicyType();
                newType.setCreatedAt(LocalDateTime.now());
                newType.setPolicyTypeCode("SOP");
                newType.setPolicyTypeName("Standard Operating Procedures");
                xpolicyRepository.createPolicyType(newType);
            }

            PolicyType charters = xpolicyRepository.getPolicyTypeUsingCode("CHT");
            if (charters == null) {
                PolicyType newType = new PolicyType();
                newType.setCreatedAt(LocalDateTime.now());
                newType.setPolicyTypeCode("CHT");
                newType.setPolicyTypeName("Charters");
                xpolicyRepository.createPolicyType(newType);
            }

            PolicyType framework = xpolicyRepository.getPolicyTypeUsingCode("COM");
            if (framework == null) {
                PolicyType newType = new PolicyType();
                newType.setCreatedAt(LocalDateTime.now());
                newType.setPolicyTypeCode("COM");
                newType.setPolicyTypeName("Committees And Association");
                xpolicyRepository.createPolicyType(newType);
            }

            PolicyType form = xpolicyRepository.getPolicyTypeUsingCode("FRM");
            if (form == null) {
                PolicyType newType = new PolicyType();
                newType.setCreatedAt(LocalDateTime.now());
                newType.setPolicyTypeCode("FRM");
                newType.setPolicyTypeName("Forms");
                xpolicyRepository.createPolicyType(newType);
            }

            PolicyType standard = xpolicyRepository.getPolicyTypeUsingCode("TEM");
            if (standard == null) {
                PolicyType newType = new PolicyType();
                newType.setCreatedAt(LocalDateTime.now());
                newType.setPolicyTypeCode("TEM");
                newType.setPolicyTypeName("Templates");
                xpolicyRepository.createPolicyType(newType);
            }

            PolicyType schedule = xpolicyRepository.getPolicyTypeUsingCode("SCH");
            if (schedule == null) {
                PolicyType newType = new PolicyType();
                newType.setCreatedAt(LocalDateTime.now());
                newType.setPolicyTypeCode("SCH");
                newType.setPolicyTypeName("Schedule");
                xpolicyRepository.createPolicyType(newType);
            }

            PolicyType report = xpolicyRepository.getPolicyTypeUsingCode("RPT");
            if (report == null) {
                PolicyType newType = new PolicyType();
                newType.setCreatedAt(LocalDateTime.now());
                newType.setPolicyTypeCode("RPT");
                newType.setPolicyTypeName("Reports");
                xpolicyRepository.createPolicyType(newType);
            }
        }

        //Add App Roles
        Map<String, String> appRoles = new HashMap<>();
        appRoles.put("LIST_POLICY", "List Policies");
        appRoles.put("UPDATE_POLICY", "Update Policies");
        appRoles.put("DELETE_POLICY", "Delete Policies");
        appRoles.put("ADD_POLICY", "Add Policies");
        appRoles.put("APPROVE_POLICY", "Approve Policies");
        appRoles.put("REVIEW_POLICY", "Approve Policies");
        appRoles.put("MANAGE_USER", "Manage Application Users");
        appRoles.put("MANAGE_ROLES", "Manage Application Roles");
        appRoles.put("GENERATE_REPORT", "Generate Report");
        appRoles.put("POLICY", "View Policies accross entities");
        appRoles.put("SOP", "View Standard Operating Procedures accross entities");
        appRoles.put("CHARTER", "View Charters accross entities");
        appRoles.put("FORMS", "View Forms accross entities");
        appRoles.put("TEMPLATES", "View Templates accross entities");
        appRoles.put("COMMITTEES AND ASSOCIATION", "View Committees and Association accross entities");
        appRoles.put("SCHEDULE", "View Schedules accross entities");
        appRoles.put("REPORT", "View Reports accross entities");

        for (Map.Entry<String, String> role : appRoles.entrySet()) {
            //Check if the role exist already
            AppRoles appRole = xpolicyRepository.getRoleUsingRoleName(role.getKey());
            if (appRole == null) {
                AppRoles newRole = new AppRoles();
                newRole.setRoleName(role.getKey());
                newRole.setRoleDesc(role.getValue());
                xpolicyRepository.createAppRole(newRole);
            }
        }

        //Create the Admin Role Group
        RoleGroups adminRole = xpolicyRepository.getRoleGroupUsingGroupName("ADMIN");
        RoleGroups adminGroup = null;
        if (adminRole == null) {
            RoleGroups newGroup = new RoleGroups();
            newGroup.setCreatedAt(LocalDateTime.now());
            newGroup.setGroupName("ADMIN");
            adminGroup = xpolicyRepository.createRoleGroup(newGroup);
        }

        RoleGroups userRole = xpolicyRepository.getRoleGroupUsingGroupName("USER");
        RoleGroups userGroup = null;
        if (userRole == null) {
            RoleGroups newGroup = new RoleGroups();
            newGroup.setCreatedAt(LocalDateTime.now());
            newGroup.setGroupName("USER");
            userGroup = xpolicyRepository.createRoleGroup(newGroup);
        }

        RoleGroups policyUploadRole = xpolicyRepository.getRoleGroupUsingGroupName("POLICY UPLOAD");
        RoleGroups policyUploadGroup = null;
        if (policyUploadRole == null) {
            RoleGroups newGroup = new RoleGroups();
            newGroup.setCreatedAt(LocalDateTime.now());
            newGroup.setGroupName("POLICY UPLOAD");
            policyUploadGroup = xpolicyRepository.createRoleGroup(newGroup);
        }

        //Check the group roles
        List<GroupRoles> adminGroupRoles = xpolicyRepository.getGroupRolesUsingRoleGroup(adminGroup);
        if (adminGroupRoles == null) {
            AppRoles appRole = xpolicyRepository.getRoleUsingRoleName("MANAGE_POLICY");
            if (appRole != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRole);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleUser = xpolicyRepository.getRoleUsingRoleName("MANAGE_USER");
            if (appRoleUser != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleUser);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleReport = xpolicyRepository.getRoleUsingRoleName("GENERATE_REPORT");
            if (appRoleReport != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleReport);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRolePolicy = xpolicyRepository.getRoleUsingRoleName("POLICY");
            if (appRolePolicy != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRolePolicy);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleSOP = xpolicyRepository.getRoleUsingRoleName("SOP");
            if (appRoleSOP != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleSOP);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleStandard = xpolicyRepository.getRoleUsingRoleName("COMMITTEES AND ASSOCIATION");
            if (appRoleStandard != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleStandard);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleFramework = xpolicyRepository.getRoleUsingRoleName("TEMPLATES");
            if (appRoleFramework != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleFramework);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleSchedule = xpolicyRepository.getRoleUsingRoleName("SCHEDULE");
            if (appRoleSchedule != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleSchedule);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleCharter = xpolicyRepository.getRoleUsingRoleName("CHARTER");
            if (appRoleCharter != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleCharter);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleForm = xpolicyRepository.getRoleUsingRoleName("FORM");
            if (appRoleForm != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleForm);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyList = xpolicyRepository.getRoleUsingRoleName("LIST_POLICY");
            if (appRoleListPolicyList != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyList);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyApprove = xpolicyRepository.getRoleUsingRoleName("APPROVE_POLICY");
            if (appRoleListPolicyApprove != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyApprove);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyDelete = xpolicyRepository.getRoleUsingRoleName("DELETE_POLICY");
            if (appRoleListPolicyDelete != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyDelete);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(adminGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
        }

        List<GroupRoles> userGroupRoles = xpolicyRepository.getGroupRolesUsingRoleGroup(userGroup);
        if (userGroupRoles == null) {
            AppRoles appRolePolicy = xpolicyRepository.getRoleUsingRoleName("POLICY");
            if (appRolePolicy != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRolePolicy);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleSOP = xpolicyRepository.getRoleUsingRoleName("SOP");
            if (appRoleSOP != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleSOP);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleStandard = xpolicyRepository.getRoleUsingRoleName("COMMITTEES AND ASSOCIATION");
            if (appRoleStandard != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleStandard);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleFramework = xpolicyRepository.getRoleUsingRoleName("TEMPLATES");
            if (appRoleFramework != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleFramework);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleSchedule = xpolicyRepository.getRoleUsingRoleName("SCHEDULE");
            if (appRoleSchedule != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleSchedule);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleCharter = xpolicyRepository.getRoleUsingRoleName("CHARTER");
            if (appRoleCharter != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleCharter);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleForm = xpolicyRepository.getRoleUsingRoleName("FORM");
            if (appRoleForm != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleForm);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(userGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
        }

        List<GroupRoles> policyUploadGroupRoles = xpolicyRepository.getGroupRolesUsingRoleGroup(policyUploadGroup);
        if (policyUploadGroupRoles == null) {
            AppRoles appRolePolicy = xpolicyRepository.getRoleUsingRoleName("POLICY");
            if (appRolePolicy != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRolePolicy);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleSOP = xpolicyRepository.getRoleUsingRoleName("SOP");
            if (appRoleSOP != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleSOP);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleStandard = xpolicyRepository.getRoleUsingRoleName("COMMITTEES AND ASSOCIATION");
            if (appRoleStandard != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleStandard);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleFramework = xpolicyRepository.getRoleUsingRoleName("TEMPLATES");
            if (appRoleFramework != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleFramework);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleSchedule = xpolicyRepository.getRoleUsingRoleName("SCHEDULE");
            if (appRoleSchedule != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleSchedule);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }

            AppRoles appRoleCharter = xpolicyRepository.getRoleUsingRoleName("CHARTER");
            if (appRoleCharter != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleCharter);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleForm = xpolicyRepository.getRoleUsingRoleName("FORM");
            if (appRoleForm != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleForm);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyList = xpolicyRepository.getRoleUsingRoleName("LIST_POLICY");
            if (appRoleListPolicyList != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyList);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyAdd = xpolicyRepository.getRoleUsingRoleName("ADD_POLICY");
            if (appRoleListPolicyAdd != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyAdd);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyDelete = xpolicyRepository.getRoleUsingRoleName("DELETE_POLICY");
            if (appRoleListPolicyDelete != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyDelete);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
            AppRoles appRoleListPolicyUpdate = xpolicyRepository.getRoleUsingRoleName("UPDATE_POLICY");
            if (appRoleListPolicyUpdate != null) {
                GroupRoles newGroupRole = new GroupRoles();
                newGroupRole.setAppRole(appRoleListPolicyUpdate);
                newGroupRole.setCreatedAt(LocalDateTime.now());
                newGroupRole.setRoleGroup(policyUploadGroup);
                xpolicyRepository.createGroupRoles(newGroupRole);
            }
        }

    }

}
