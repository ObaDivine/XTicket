var options = new List('dealsTable', {
    valueNames: ['email', 'username', 'company', 'division', 'department', 'policyChampion', 'accessLevel', 'dateCreated', 'role',
        'policy', 'policycode', 'company', 'lastReview', 'fileSize', 'underReview', 'actionBy', 'auditAction', 'auditCategory',
        'auditClass', 'oldValue', 'newValue',
        'policyName', 'policyType', 'policyDescription', 'documentId', 'policyAuthor', 'expiryDate', 'lastReview',
        'description', 'name', 'reviewBy', 'reviewAt', 'comment'
    ],
    page: 10,
    pagination: true
});

var policyList = new List('dealsTable', options);

function deleteRole(id) {
    $('#deleteRole').attr("href", "/xticket/user/roles/delete?seid=" + id);
    $('#roleDeleteModal').modal('show');
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
;

function fetchTicketType() {
    let ticketGroup = document.getElementById("ticketGroupCode").value;
    $.ajax({
        url: "/xticket/ticket/type/fetch/" + ticketGroup,
        type: "GET",
        dataType: "JSON",
        success: function (data) {
            var html = '<option value="">---Select Ticket Type---</option>';
            var len = data.length;
            for (var i = 0; i < len; i++) {
                html += '<option value="' + data[i].ticketTypeCode + '">' + data[i].ticketTypeName + '</option>';
            }
            html += '</option>';
            $("#ticketTypeCode").html(html);
        },
        error: function (xhr, status) {
            alert(xhr);
        }
    });
};




