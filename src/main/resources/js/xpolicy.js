function fetchDivision() {
    let company = document.getElementById("company").value;
    $.ajax({
        url: "/xpolicy/division/fetch/" + company,
        type: "GET",
        dataType: "json",
        success: function (data) {
            var html = '<option value="">--Select Division --</option>';
            var len = data.length;
            for (var i = 0; i < len; i++) {
                html += '<option value="' + data[i].id + '">' + data[i].divisionName + '</option>';
            }
            html += '</option>';
            $('#division').html(html);
        },
        error: function (xhr, status) {
            alert(xhr);
        }
    });
}
;
function fetchDepartment() {
    let division = document.getElementById("division").value;
    $.ajax({
        url: "/xpolicy/department/fetch/" + division,
        type: "GET",
        dataType: "json",
        success: function (data) {
            var html = '<option value="">--Select Department --</option>';
            var len = data.length;
            for (var i = 0; i < len; i++) {
                html += '<option value="' + data[i].id + '">' + data[i].departmentName + '</option>';
            }
            html += '</option>';
            $('#department').html(html);
        },
        error: function (xhr, status) {
            alert(xhr);
        }
    });
}
;
function setPolicy(policyType) {
    $('#policyType').val(policyType);
}
;

function fetchPolicyDetails(id) {
    $.ajax({
        url: "/xpolicy/policy/details/" + id,
        type: "GET",
        dataType: "json",
        success: function (data) {
            let dateDiff = (new Date() - new Date(data.lastReview)) / (1000 * 3600 * 24);
            $('#policyDetailsName').text(data.policyName);
            $('#policyDetailsCompany').text(data.company.companyName);
            $('#policyDetailsDivision').text(data.division.divisionName);
            $('#policyDetailsDepartment').text(data.department.departmentName);
            $('#policyDetailsLastReview').text(new Date(data.lastReview));
            $('#policyDetailsDescription').text(data.policyDescription);
            $('#policyDetailsAuthor').text(data.policyAuthor);
            $('#policyDetailsCode').text(data.policyCode);
            $('#policyDetailsDocumentId').text(data.policyDocumentId);
            $('#policyDetailsExpiryDate').text(new Date(data.expiryDate));
            $('#daysSinceLastReview').text(Math.floor(dateDiff) + ' Days');
        },
        error: function (xhr, status) {
            alert(xhr);
        }
    });
}
;

var options = {
    valueNames: ['policy_name', 'policy_code', 'policy_type', 'description', 'document_id', 'company', 'division', 'department', 'author', 'date_created', 'expiry_date', 'last_review', 'under_review', 'access_level']
};
var policyList = new List('dealsTable', options);

function fetchUpdateDivision(company, divisionToSet) {
    let companyToSearch = "";
    if (company === 0) {
        companyToSearch = document.getElementById("updateCompany").value;
    } else {
        companyToSearch = company;
    }
    ;

    if (companyToSearch !== null && companyToSearch !== '') {
        $.ajax({
            url: "/xpolicy/division/fetch/" + companyToSearch,
            type: "GET",
            dataType: "json",
            success: function (data) {
                var html = '<option value="">--Select Division --</option>';
                var len = data.length;
                for (var i = 0; i < len; i++) {
                    html += '<option value="' + data[i].id + '">' + data[i].divisionName + '</option>';
                }
                html += '</option>';
                $('#updateDivision').html(html);
                if (divisionToSet !== 0) {
                    $('#updateDivision').val(divisionToSet);
                }
                ;
            },
            error: function (xhr, status) {
                alert(xhr);
            }
        });
    }
}
;
function fetchUpdateDepartment(division, departmentToSet) {
    let divisionToSearch = "";
    if (division === 0) {
        divisionToSearch = document.getElementById("updateDivision").value;
    } else {
        divisionToSearch = division;
    }
    ;

    if (divisionToSearch !== null && divisionToSearch !== '') {
        $.ajax({
            url: "/xpolicy/department/fetch/" + divisionToSearch,
            type: "GET",
            dataType: "json",
            success: function (data) {
                var html = '<option value="">--Select Department --</option>';
                var len = data.length;
                for (var i = 0; i < len; i++) {
                    html += '<option value="' + data[i].id + '">' + data[i].departmentName + '</option>';
                }
                html += '</option>';
                $('#updateDepartment').html(html);
                if (departmentToSet !== 0) {
                    $('#updateDepartment').val(departmentToSet);
                }
                ;
            },
            error: function (xhr, status) {
                alert(xhr);
            }
        });
    }
}
;

function approvePolicy(id) {
    $('#approvePolicy').attr("href", "/xpolicy/admin/policy/approve/" + id);
    $('#policyApprovalModal').modal('show');
}
;

function deletePolicy(id) {
    $('#deletePolicy').attr("href", "/xpolicy/admin/policy/delete/" + id);
    $('#policyDeleteModal').modal('show');
}
;

function declinePolicy(id) {
    $('#reference').val(id);
    $('#policyDeclineModal').modal('show');
}
;

function deleteUser(id) {
    $('#deleteUser').attr("href", "/xpolicy/admin/users/delete/" + id);
    $('#userDeleteModal').modal('show');
}
;

function deleteRole(id) {
    $('#deleteRole').attr("href", "/xpolicy/admin/roles/delete/" + id);
    $('#roleDeleteModal').modal('show');
}
;
function tableToCSV() {
    // Variable to store the final csv data
    var csv_data = [];

    // Get each row data
    var rows = document.querySelectorAll("table tbody tr");
    for (var i = 0; i < rows.length; i++) {

        // Get each column data
        var cols = rows[i].querySelectorAll('td,th');

        // Stores each csv row data
        var csvrow = [];
        for (var j = 0; j < cols.length; j++) {

            // Get the text data of each cell
            // of a row and push it to csvrow
            csvrow.push(cols[j].innerHTML);
        }

        // Combine each column value with comma
        csv_data.push(csvrow.join(","));
    }

    // Combine each row data with new line character
    csv_data = csv_data.join('\n');

    // Call this function to download csv file 
    downloadCSVFile(csv_data);
}
;

function downloadCSVFile(csv_data) {
    // Create CSV file object and feed our csv_data into it
    CSVFile = new Blob([csv_data], {
        type: "text/csv"
    });

    // Create to temporary link to initiate download process
    var temp_link = document.createElement('a');

    // Download csv file
    temp_link.download = "GfG.csv";
    var url = window.URL.createObjectURL(CSVFile);
    temp_link.href = url;

    // This link should not be displayed
    temp_link.style.display = "none";
    document.body.appendChild(temp_link);

    // Automatically click the link to trigger download
    temp_link.click();
    document.body.removeChild(temp_link);
}
;

$(document).ready(function () {
    var navbarTopStyle = window.config.config.phoenixNavbarTopStyle;
    var navbarTop = document.querySelector('.navbar-top');
    if (navbarTopStyle === 'darker') {
        navbarTop.classList.add('navbar-darker');
    }

    var navbarVerticalStyle = window.config.config.phoenixNavbarVerticalStyle;
    var navbarVertical = document.querySelector('.navbar-vertical');
    if (navbarVertical && navbarVerticalStyle === 'darker') {
        navbarVertical.classList.add('navbar-darker');
    }
    ;
});


var policyList = new List('dealsTable', {
    valueNames: ['email', 'username', 'company', 'division', 'department', 'policyChampion', 'accessLevel', 'dateCreated', 'role',
        'policy', 'policycode', 'company', 'lastReview', 'fileSize', 'underReview', 'actionBy', 'auditAction', 'auditCategory',
        'auditClass', 'oldValue', 'newValue',
        'policyName', 'policyType', 'policyDescription', 'documentId', 'policyAuthor', 'expiryDate', 'lastReview',
        'description', 'name', 'reviewBy', 'reviewAt', 'comment'
    ],
    page: 10,
    pagination: true
});

$('.digit-group').find('input').each(function () {
    $(this).attr('maxlength', 1);
    $(this).on('keyup', function (e) {
        var parent = $($(this).parent());

        if (e.keyCode === 8 || e.keyCode === 37) {
            var prev = parent.find('input#' + $(this).data('previous'));

            if (prev.length) {
                $(prev).select();
            }
        } else if ((e.keyCode >= 48 && e.keyCode <= 57) || (e.keyCode >= 65 && e.keyCode <= 90) || (e.keyCode >= 96 && e.keyCode <= 105) || e.keyCode === 39) {
            var next = parent.find('input#' + $(this).data('next'));

            if (next.length) {
                $(next).select();
            } else {
                if (parent.data('autosubmit')) {
                    parent.submit();
                }
            }
        }
    });
});

function preloaderFunction() {
    var myVar = setTimeout(showPage);
}
;

function showPage() {
    document.getElementById('loader').style.display = "none";
    document.getElementById('top').style.display = "block";
}
;

var navbarStyle = window.config.config.phoenixNavbarStyle;
if (navbarStyle && navbarStyle !== 'transparent') {
    document.querySelector('body').classList.add(`navbar-${navbarStyle}`);
}




