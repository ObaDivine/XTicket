(function (factory) {
  typeof define === 'function' && define.amd ? define(factory) :
  factory();
})((function () { 'use strict';

  // import * as echarts from 'echarts';
  const { merge } = window._;

  // form config.js
  const echartSetOption = (chart, userOptions, getDefaultOptions) => {
    const themeController = document.body;
    // Merge user options with lodash
    chart.setOption(merge(getDefaultOptions(), userOptions));

    themeController.addEventListener(
      'clickControl',
      ({ detail: { control } }) => {
        if (control === 'phoenixTheme') {
          chart.setOption(window._.merge(getDefaultOptions(), userOptions));
        }
      }
    );
  };
  // -------------------end config.js--------------------

  const resizeEcharts = () => {
    const $echarts = document.querySelectorAll('[data-echart-responsive]');

    if ($echarts.length > 0) {
      $echarts.forEach(item => {
        const echartInstance = echarts.getInstanceByDom(item);
        echartInstance?.resize();
      });
    }
  };

  const navbarVerticalToggle = document.querySelector('.navbar-vertical-toggle');
  navbarVerticalToggle &&
    navbarVerticalToggle.addEventListener('navbar.vertical.toggle', e => {
      return resizeEcharts();
    });


  // import * as echarts from 'echarts';
  const { echarts: echarts$2 } = window;

  /* -------------------------------------------------------------------------- */
  /*                    Ticket Open & Close Chart                               */
  /* -------------------------------------------------------------------------- */

  const ticketOpenCloseChartInit = () => {
    const { getData, getColor, resize } = window.phoenix.utils;
    const ECHART_TOP_COUPONS = '.echart-ticket-open-close';
    const $echartTicketOpenClose = document.querySelector(ECHART_TOP_COUPONS);

    if ($echartTicketOpenClose) {
      const userOptions = getData($echartTicketOpenClose, 'options');
      const chart = echarts$2.init($echartTicketOpenClose);

      const getDefaultOptions = () => ({
        color: ['green', 'red', 'yellow', 'blue'],

        tooltip: {
          trigger: 'item',
          padding: [7, 10],
          backgroundColor: getColor('gray-100'),
          borderColor: getColor('gray-300'),
          textStyle: { color: getColor('dark') },
          borderWidth: 1,
          transitionDuration: 0,
          formatter: params => {
            return `<strong>${params.data.name}:</strong> ${params.percent}%`;
          }
        },
        legend: { show: false },
        series: [
          {
            name: 'Ticket by Status',
            type: 'pie',
            radius: ['100%', '100%'],
            avoidLabelOverlap: false,
            emphasis: {
              scale: false,
              itemStyle: {
                color: 'inherit'
              }
            },
            itemStyle: {
              borderWidth: 2,
              borderColor: getColor('white')
            },
            label: {
              show: true,
              position: 'center',
              formatter: '{a}',
              fontSize: 23,
              color: getColor('dark')
            },
            data: [
              { value: 7200000, name: 'Percentage discount' },
              { value: 1800000, name: 'Fixed card discount' },
              { value: 1000000, name: 'Fixed product discount'},
              { value: 1600000, name: 'Brian Okon'}
            ]
          }
        ],
        grid: { containLabel: true }
      });

      echartSetOption(chart, userOptions, getDefaultOptions);

      resize(() => {
        chart.resize();
      });
    }
  };


  const { echarts: echarts$1 } = window;

  /* -------------------------------------------------------------------------- */
  /*                         Ticket By Group Chart                              */
  /* -------------------------------------------------------------------------- */

  const ticketGroupChartInit = () => {
    const { getData, getColor, resize } = window.phoenix.utils;
    const $chartEl = document.querySelector('.echarts-ticket-group');

    if ($chartEl) {
      const userOptions = getData($chartEl, 'options');
      const chart = echarts$1.init($chartEl);

      const getDefaultOptions = () => ({
        tooltip: {
          trigger: 'item',
          padding: [7, 10],
          backgroundColor: getColor('gray-100'),
          borderColor: getColor('gray-300'),
          textStyle: { color: getColor('dark') },
          borderWidth: 1,
          transitionDuration: 0,
          formatter: params => {
            return `<strong>${params.seriesName}:</strong> ${params.value}%`;
          }
        },
        legend: { show: false },
        series: [
          {
            type: 'gauge',
            center: ['50%', '60%'],
            name: 'Paying customer',
            startAngle: 180,
            endAngle: 0,
            min: 0,
            max: 100,
            splitNumber: 12,
            itemStyle: {
              color: getColor('primary')
            },
            progress: {
              show: true,
              roundCap: true,
              width: 12,
              itemStyle: {
                shadowBlur: 0,
                shadowColor: '#0000'
              }
            },
            pointer: {
              show: false
            },
            axisLine: {
              roundCap: true,
              lineStyle: {
                width: 12,
                color: [[1, getColor('primary-100')]]
              }
            },
            axisTick: {
              show: false
            },
            splitLine: {
              show: false
            },
            axisLabel: {
              show: false
            },
            title: {
              show: false
            },
            detail: {
              show: false
            },
            data: [
              {
                value: 30
              }
            ]
          }
        ]
      });

      echartSetOption(chart, userOptions, getDefaultOptions);

      resize(() => {
        chart.resize();
      });
    }
  };


  const { docReady } = window.phoenix.utils;
  docReady(ticketOpenCloseChartInit);
  docReady(ticketGroupChartInit);
}));
//# sourceMappingURL=ecommerce-dashboard.js.map