package com.adriel.checkmybus;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.adriel.checkmybus.api.CtbEtaServiceImpl;
import com.adriel.checkmybus.api.KmbEtaServiceImpl;
import com.adriel.checkmybus.callback.EtaReturnCallback;
import com.adriel.checkmybus.callback.EtaReturnListener;
import com.adriel.checkmybus.constants.Constants;
import com.adriel.checkmybus.model.StopETA;
import com.adriel.checkmybus.model.StopETAData;
import com.adriel.checkmybus.utils.DateUtils;
import com.adriel.checkmybus.utils.JointOperationUtils;
import com.adriel.checkmybus.utils.ValidationUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SelectTimeActivity extends AppCompatActivity {

    private TextView companyTextView;
    private TextView routeNumberTextView;
    private TextView routeDirectionTextView;
    private TextView busStopTextView;
    private TextView etaTextView;
    private Button refreshButton;
    private Button saveButton;
    private LinearLayout leftCheckBoxPanel;
    private CheckBox sundayCheckBox;
    private CheckBox mondayCheckBox;
    private CheckBox tuesdayCheckBox;
    private CheckBox wednesdayCheckBox;
    private CheckBox thursdayCheckBox;
    private CheckBox fridayCheckBox;
    private CheckBox saturdayCheckBox;
    private LinearLayout rightCheckBoxPanel;

    private boolean[] daySelected;
    private String company;
    private String routeNumber;
    private String direction;
    private String bound;
    private String busStopName;
    private String jointBusStopName;
    private String busStopId;
    private String jointBusStopId;
    private String busStopSeq;
    private int etaCount;
    private final List<StopETAData> stopETAList = Collections.synchronizedList(new ArrayList<>());;

    private Bundle params;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_time);

        companyTextView = findViewById(R.id.companySelectTimeTextView);
        routeNumberTextView = findViewById(R.id.routeNumberSelectTimeTextView);
        routeDirectionTextView = findViewById(R.id.routeDirectionSelectTimeTextView);
        busStopTextView = findViewById(R.id.busStopTextView);
        etaTextView = findViewById(R.id.etaTextView);
        refreshButton = findViewById(R.id.refreshButton);
        saveButton = findViewById(R.id.saveButton);
        leftCheckBoxPanel = findViewById(R.id.leftCheckboxPanel);
        rightCheckBoxPanel = findViewById(R.id.rightCheckboxPanel);
        sundayCheckBox = findViewById(R.id.sundayCheckBox);
        mondayCheckBox = findViewById(R.id.mondayCheckBox);
        tuesdayCheckBox = findViewById(R.id.tuesdayCheckBox);
        wednesdayCheckBox = findViewById(R.id.wednesdayCheckBox);
        thursdayCheckBox = findViewById(R.id.thursdayCheckBox);
        fridayCheckBox = findViewById(R.id.fridayCheckBox);
        saturdayCheckBox = findViewById(R.id.saturdayCheckBox);

        daySelected = new boolean[7];
        params = getIntent().getExtras();
        company = params.getString(getString(R.string.bundle_company));
        companyTextView.setText(company.toUpperCase(Locale.ROOT));
        direction = params.getString(getString(R.string.bundle_direction));
        routeDirectionTextView.setText(direction);
        bound = params.getString(getString(R.string.bundle_bound));
        routeNumber = params.getString(getString(R.string.bundle_route_num));
        routeNumberTextView.setText(routeNumber);
        busStopName = params.getString(getString(R.string.bundle_stop_name));
        busStopId = params.getString(getString(R.string.bundle_stop_id));
        jointBusStopId = params.getString(getString(R.string.bundle_joint_stop_id));
        jointBusStopName = params.getString(getString(R.string.bundle_joint_stop_name));
        busStopSeq = params.getString(getString(R.string.bundle_stop_seq));
        busStopTextView.setText(String.format("%s. %s", busStopSeq, busStopName));

        saveButton.setOnClickListener(this::showTimePickerDialog);
        refreshButton.setOnClickListener(view -> getNextEta(busStopId, jointBusStopId));

        sundayCheckBox.setOnClickListener(this::onCheckedChanged);
        mondayCheckBox.setOnClickListener(this::onCheckedChanged);
        tuesdayCheckBox.setOnClickListener(this::onCheckedChanged);
        wednesdayCheckBox.setOnClickListener(this::onCheckedChanged);
        thursdayCheckBox.setOnClickListener(this::onCheckedChanged);
        fridayCheckBox.setOnClickListener(this::onCheckedChanged);
        saturdayCheckBox.setOnClickListener(this::onCheckedChanged);

        getNextEta(busStopId, jointBusStopId);
    }

    private void onCheckedChanged(View v) {
        boolean checked = ((CheckBox) v).isChecked();

        switch (v.getId()) {
            case R.id.sundayCheckBox:
                daySelected[0] = checked;
                break;
            case R.id.mondayCheckBox:
                daySelected[1] = checked;
                break;
            case R.id.tuesdayCheckBox:
                daySelected[2] = checked;
                break;
            case R.id.wednesdayCheckBox:
                daySelected[3] = checked;
                break;
            case R.id.thursdayCheckBox:
                daySelected[4] = checked;
                break;
            case R.id.fridayCheckBox:
                daySelected[5] = checked;
                break;
            case R.id.saturdayCheckBox:
                daySelected[6] = checked;
                break;
            default:
                break;
        }
    }

    private void showTimePickerDialog(View v) {
        for (boolean day : daySelected) {
            if (day) {
                DialogFragment timePickerFragment = new TimePickerFragment(this::timePickerCallback);
                timePickerFragment.show(getSupportFragmentManager(), "timePicker");
                return;
            }
        }

        Toast.makeText(getApplicationContext(), R.string.no_checkbox_checked,
                Toast.LENGTH_LONG).show();
    }

    private void timePickerCallback(int hourPicked, int minPicked) {
        Log.d("Hour", String.valueOf(hourPicked));
        Log.d("Minute", String.valueOf(minPicked));
    }

    private void getNextEta(String stopId, String jointStopId) {
        etaCount = 0;
        // 4 - Find ETA by stop ID
        EtaReturnListener<EtaReturnCallback<StopETA>> stopEtaCallbackListener =
                this::stopEtaCallbackListener;

        switch (company) {
            case Constants.CTB_COMPANY:
            case Constants.NWFB_COMPANY:
                CtbEtaServiceImpl ctbEtaService = CtbEtaServiceImpl.getInstance();
                ctbEtaService.getStopEtaById(company, stopId, routeNumber, stopEtaCallbackListener);
                etaCount++;
                break;
            case Constants.KMB_COMPANY:
                KmbEtaServiceImpl kmbEtaService = KmbEtaServiceImpl.getInstance();
                kmbEtaService.getStopEtaById(stopId, stopEtaCallbackListener);
                etaCount++;
                break;
            default:
                ctbEtaService = CtbEtaServiceImpl.getInstance();
                ctbEtaService.getStopEtaById(JointOperationUtils.getCtbNwfbFromJointOpRoute(company),
                        jointStopId, routeNumber, stopEtaCallbackListener);
                etaCount++;
                kmbEtaService = KmbEtaServiceImpl.getInstance();
                kmbEtaService.getStopEtaById(stopId, stopEtaCallbackListener);
                etaCount++;
        }
    }

    private void stopEtaCallbackListener(EtaReturnCallback<StopETA> stopEtaCallback) {
        int returnCode = ValidationUtils.validateStopEtaCallback(stopEtaCallback);
        switch (returnCode) {
            case Constants.CALLBACK_EXCEPTION:
                Toast.makeText(getApplicationContext(), getString(R.string.application_exception),
                        Toast.LENGTH_LONG).show();
                break;
            case Constants.CALLBACK_EMPTY_OUTPUT:
                break;
            default:
                // Each stop has multiple routes, each route has multiple departures
                // For terminus, each route will have ETA of both directions
                // Get selected route number, direction, stop seq number closest to and smaller than selected
                // (in CTB ETA, if more than 1 possible routes for a number, stop seq number in RouteStop
                // API may be greater than ETA API since it contains some unused stops),
                // and smallest ETA seq
                StopETAData selectedStopETA = null;
                for (StopETAData stopETA : stopEtaCallback.getEtaReturnModel().getData()) {
                    // Cater for jointly operated cross harbour routes, bound is opposite for joint company
                    boolean boundMatch = (JointOperationUtils.isJointCrossHarbourRoute(company, routeNumber)
                            && JointOperationUtils.getCtbNwfbFromJointOpRoute(company).equalsIgnoreCase(stopETA.getCo())
                            && !bound.substring(0, 1).equalsIgnoreCase(stopETA.getDir())) ||
                            (JointOperationUtils.isJointCrossHarbourRoute(company, routeNumber)
                                    && !JointOperationUtils.getCtbNwfbFromJointOpRoute(company).equalsIgnoreCase(stopETA.getCo())
                                    && bound.substring(0, 1).equalsIgnoreCase(stopETA.getDir())) ||
                            (!JointOperationUtils.isJointCrossHarbourRoute(company, routeNumber)
                                    && bound.substring(0, 1).equalsIgnoreCase(stopETA.getDir()));
                    if (!routeNumber.equals(stopETA.getRoute()) ||
                            !boundMatch ||
                            stopETA.getSeq() > Integer.parseInt(busStopSeq) ||
                            stopETA.getEta() == null || stopETA.getEta().isEmpty()) {
                        continue;
                    }

                    // Get a larger stop seq (closer to selected stop seq)
                    // or a smaller ETA seq (earlier) only if same stop seq
                    if (selectedStopETA == null ||
                            stopETA.getSeq() > selectedStopETA.getSeq() ||
                            (stopETA.getSeq() == selectedStopETA.getSeq() &&
                                    stopETA.getEta_seq() < selectedStopETA.getEta_seq())) {
                        selectedStopETA = stopETA;
                    }
                }

                if (selectedStopETA != null) {
                    stopETAList.add(selectedStopETA);
                }
        }

        if (--etaCount == 0) {
            processStopETAList();
        }
    }

    private void processStopETAList() {
        Date earliestEta = null;
        String etaDestination = null;
        String etaCompany = null;
        String etaStopName = null;

        synchronized (stopETAList) {
            for (StopETAData stopETA : stopETAList) {
                try {
                    if (earliestEta == null || stopETA.getEtaDateTime().before(earliestEta)) {
                        earliestEta = stopETA.getEtaDateTime();
                        etaDestination = stopETA.getDest_en();
                        etaCompany = stopETA.getCo();
                        etaStopName = (JointOperationUtils.isJointOpRoute(company) &&
                                JointOperationUtils.isCtbNwfbOperator(etaCompany)) ?
                                jointBusStopName : busStopName;
                    }
                } catch (ParseException e) {
                    etaTextView.setText(getString(R.string.application_exception));
                    return;
                }
            }
        }

        if (earliestEta != null) {
            long timeFromNowMs = Math.abs(earliestEta.getTime() - Calendar.getInstance().getTime().getTime());
            long timeFromNowMins = TimeUnit.MINUTES.convert(timeFromNowMs, TimeUnit.MILLISECONDS);

            busStopTextView.setText(String.format("%s. %s", busStopSeq, etaStopName));
            companyTextView.setText(etaCompany);
            etaTextView.setText(String.format(getString(R.string.eta_description),
                    DateUtils.convertDateToString(earliestEta, Constants.TIME_FORMATTER), timeFromNowMins));
            routeDirectionTextView.setText(etaDestination);
        } else {
            etaTextView.setText(getString(R.string.eta_not_found));
        }
    }

}