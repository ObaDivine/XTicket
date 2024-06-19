var options = new List('listTable', {
    valueNames: ['date', 'requestBy', 'ticketId', 'ticketGroup', 'ticketType', 'ticketCount', 'priority', 'slaExpiry', 'sla', 'slaViolated', 'reopened', 'subject',
        'dateClosed', 'closedBy', 'createdBy', 'name', 'email', 'mobile', 'gender', 'internal', 'agent', 'locked', 'role', 'lastLogin', 'timeElapsed', 'reassigned',
        'reassignedAt', 'reassignedBy', 'reassignedTo', 'initialSla', 'newSla', 'escalation', 'escalated', 'status', 'group', 'code', 'requireCR', 'requireSR',
        'serviceUnit', 'entityName'
    ],
    page: 10,
    pagination: true
});

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
}
;

function fetchTicketAgent() {
    let ticketType = document.getElementById("ticketTypeCode").value;
    $.ajax({
        url: "/xticket/ticket/agent/fetch/" + ticketType,
        type: "GET",
        dataType: "JSON",
        success: function (data) {
            var html = '<option value="">---Select Ticket Agent---</option>';
            var len = data.length;
            for (var i = 0; i < len; i++) {
                html += '<option value="' + data[i].agent.email + '">' + data[i].agent.lastName + ', ' + data[i].agent.otherName + '</option>';
            }
            html += '</option>';
            $("#email").html(html);
        },
        error: function (xhr, status) {
            alert(xhr);
        }
    });
}
;

function emailValidation() {
    let userEmail = $("#email").val();
    let adDomains = $("#adAuthDomains").val().split(',');
    let adAuth = false;
    for (let i = 0; i < adDomains.length; i++) {
        if (userEmail.toString().includes(adDomains[i])) {
            adAuth = true;
        }
    }

    if (adAuth) {
        $('#pwd').hide();
        $('#password').val('w6W;;%LYcwUjfh');
        $('#confirmPassword').val('w6W;;%LYcwUjfh');
        $('#serviceUnit').show();
    } else {
        $('#pwd').show();
        $('#serviceUnit').hide();
    }
    ;
}
;

function replaceString(text, charToReplace) {
    var newString = text.replace(charToReplace, '');
    return newString;
}
;

