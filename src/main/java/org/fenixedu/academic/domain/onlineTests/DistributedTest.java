/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Core.
 *
 * FenixEdu Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Core.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Created on 19/Ago/2003
 */

package org.fenixedu.academic.domain.onlineTests;

import org.fenixedu.academic.domain.EvaluationManagementLog;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.tests.Response;
import org.fenixedu.academic.util.tests.TestType;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.FenixFramework;

import java.util.*;

/**
 * @author Susana Fernandes
 */
public class DistributedTest extends DistributedTest_Base {

    public static final Comparator<DistributedTest> COMPARATOR_BY_DATE = (dt1, dt2) -> {
        final int b = dt1.getBeginDate().compareTo(dt2.getBeginDate());
        final int h = dt1.getBeginHour().compareTo(dt2.getBeginHour());
        return b == 0 ? (h == 0 ? dt1.getExternalId().compareTo(dt2.getExternalId()) : h) : b;
    };

    public DistributedTest() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    @Override
    public java.lang.String getEvaluationTitle() {
        String evaluationTitle = super.getEvaluationTitle();
        if (evaluationTitle == null || evaluationTitle.length() == 0) {
            return getTitle();
        }
        return evaluationTitle;
    }

    public boolean canBeDelete() {
        return getTestType().getType().intValue() != TestType.EVALUATION || countResponses(null, true) == 0;
    }
    
    public void delete() {
        if (!canBeDelete()) {
            throw new DomainException("error.delete.evaluationTestWithResponses");
        }
        ExecutionCourse ec = getTestScope().getExecutionCourse();
        EvaluationManagementLog.createLog(ec, Bundle.MESSAGING, "log.executionCourse.evaluation.tests.distribution.removed",
                getEvaluationTitle(), getBeginDateTimeFormatted(), ec.getName(), ec.getDegreePresentationString());

        for (StudentTestQuestion studentTestQuestion : getDistributedTestQuestionsSet()) {
                Question question = studentTestQuestion.getQuestion();
                studentTestQuestion.delete();
                question.deleteIfNotUsed();
        }
        
        for (; !getStudentsLogsSet().isEmpty(); getStudentsLogsSet().iterator().next().delete()) {
            ;
        }
        if (getTestType().getType().intValue() == TestType.EVALUATION) {
            getOnlineTest().delete();
        }

        setTestScope(null);
        setRootDomainObject(null);

        deleteDomainObject();
    }

    public Calendar getBeginDate() {
        if (getBeginDateDate() != null) {
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(getBeginDateDate());
            return calendar;
        }

        return null;
    }

    public void setBeginDate(Calendar beginDate) {
        final Date date = (beginDate != null) ? beginDate.getTime() : null;
        setBeginDateDate(date);
    }

    public Calendar getBeginHour() {
        if (getBeginHourDate() != null) {
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(getBeginHourDate());
            return calendar;
        }

        return null;
    }

    public void setBeginHour(Calendar beginHour) {
        final Date date = (beginHour != null) ? beginHour.getTime() : null;
        setBeginHourDate(date);
    }

    public Calendar getEndDate() {
        if (getEndDateDate() != null) {
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(getEndDateDate());
            return calendar;
        }

        return null;
    }

    public void setEndDate(Calendar endDate) {
        final Date date = (endDate != null) ? endDate.getTime() : null;
        setEndDateDate(date);
    }

    public Calendar getEndHour() {
        if (getEndHourDate() != null) {
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(getEndHourDate());
            return calendar;
        }

        return null;
    }

    public void setEndHour(Calendar endHour) {
        final Date date = (endHour != null) ? endHour.getTime() : null;
        setEndHourDate(date);
    }

    public StudentTestLog getLastSubmissionStudentTestLog(final String registrationId) {
        Registration registration = FenixFramework.getDomainObject(registrationId);
        for (final StudentTestLog studentTestLog : this.getStudentsLogsSet()) {
            if (studentTestLog.getEvent().startsWith("Submeter Teste;") && registration.equals(studentTestLog.getStudent())) {
                return studentTestLog;
            }
        }
        return null;
    }

    public List<StudentTestLog> getStudentTestLogs(final Registration registration) {
        List<StudentTestLog> result = new ArrayList<StudentTestLog>();
        for (final StudentTestLog studentTestLog : this.getStudentsLogsSet()) {
            if (studentTestLog.getStudent().equals(registration)) {
                result.add(studentTestLog);
            }
        }
        return result;
    }

    public List<StudentTestQuestion> getStudentTestQuestionsSortedByStudentNumberAndTestQuestionOrder() {
        final List<StudentTestQuestion> studentTestQuestions =
                new ArrayList<StudentTestQuestion>(getDistributedTestQuestionsSet());
        Collections.sort(studentTestQuestions, StudentTestQuestion.COMPARATOR_BY_STUDENT_NUMBER_AND_TEST_QUESTION_ORDER);
        return studentTestQuestions;
    }

    public Set<StudentTestQuestion> findStudentTestQuestionsByTestQuestionOrder(final Integer order) {
        final Set<StudentTestQuestion> studentTestQuestions = new HashSet<StudentTestQuestion>();
        for (final StudentTestQuestion studentTestQuestion : getDistributedTestQuestionsSet()) {
            if (studentTestQuestion.getTestQuestionOrder().equals(order)) {
                studentTestQuestions.add(studentTestQuestion);
            }
        }
        return studentTestQuestions;
    }

    public Set<Registration> findStudents() {
        final SortedSet<Registration> students = new TreeSet<Registration>(Registration.COMPARATOR_BY_NUMBER_AND_ID);
        for (final StudentTestQuestion studentTestQuestion : getDistributedTestQuestionsSet()) {
            students.add(studentTestQuestion.getStudent());
        }
        return students;
    }

    public SortedSet<StudentTestQuestion> findStudentTestQuestionsOfFirstStudentOrderedByTestQuestionOrder() {
        final SortedSet<StudentTestQuestion> studentTestQuestions =
                new TreeSet<StudentTestQuestion>(StudentTestQuestion.COMPARATOR_BY_TEST_QUESTION_ORDER);
        final Registration registration =
                getDistributedTestQuestionsSet() != null ? getDistributedTestQuestionsSet().iterator().next().getStudent() : null;
        for (final StudentTestQuestion studentTestQuestion : getDistributedTestQuestionsSet()) {
            if (registration == studentTestQuestion.getStudent()) {
                studentTestQuestions.add(studentTestQuestion);
            }
        }
        return studentTestQuestions;
    }

    public SortedSet<StudentTestQuestion> findStudentTestQuestions(final Registration registration) {
        final SortedSet<StudentTestQuestion> studentTestQuestions =
                new TreeSet<StudentTestQuestion>(StudentTestQuestion.COMPARATOR_BY_TEST_QUESTION_ORDER);
        for (final StudentTestQuestion studentTestQuestion : getDistributedTestQuestionsSet()) {
            if (registration == studentTestQuestion.getStudent()) {
                studentTestQuestions.add(studentTestQuestion);
            }
        }
        return studentTestQuestions;
    }

    public Double calculateMaximumDistributedTestMark() {
        double result = 0;
        for (final StudentTestQuestion studentTestQuestion : findStudentTestQuestionsOfFirstStudentOrderedByTestQuestionOrder()) {
            result += studentTestQuestion.getTestQuestionValue().doubleValue();
        }
        return Double.valueOf(result);
    }

    public Double calculateTestFinalMarkForStudent(final Registration registration) {
        double result = 0;
        for (final StudentTestQuestion studentTestQuestion : getDistributedTestQuestionsSet()) {
            if (registration == studentTestQuestion.getStudent()) {
                result += studentTestQuestion.getTestQuestionMark().doubleValue();
            }
        }
        return Double.valueOf(result);
    }

    public int countLikeResponses(final Integer order, final Response response) {
        int count = 0;
        // for (final StudentTestQuestion studentTestQuestion :
        // getDistributedTestQuestionsSet()) {
        // if (studentTestQuestion.getTestQuestionOrder().equals(order)
        // && studentTestQuestion.getResponse().contains(response)) {
        // count++;
        // }
        // }
        return count;
    }

    public int countResponses(final Integer order, final String response) {
        int count = 0;
        for (final StudentTestQuestion studentTestQuestion : getDistributedTestQuestionsSet()) {
            if (studentTestQuestion.getResponse() != null && studentTestQuestion.getTestQuestionOrder().equals(order)
                    && studentTestQuestion.getResponse().hasResponse(response)) {
                count++;
            }
        }
        return count;
    }

    public Set<Response> findResponses() {
        final Set<Response> responses = new HashSet<Response>();
        for (final StudentTestQuestion studentTestQuestion : getDistributedTestQuestionsSet()) {
            if (studentTestQuestion.getResponse() != null) {
                responses.add(studentTestQuestion.getResponse());
            }
        }
        return responses;
    }

    public int countResponses(final Integer order, final boolean responded) {
        int count = 0;
        for (final StudentTestQuestion studentTestQuestion : getDistributedTestQuestionsSet()) {
            if (order == null || studentTestQuestion.getTestQuestionOrder().equals(order)) {
                if (responded && studentTestQuestion.getResponse() != null) {
                    count++;
                } else if (!responded && studentTestQuestion.getResponse() == null) {
                    count++;
                }
            }
        }
        return count;
    }

    public int countAnsweres(final Integer order, final double mark, final boolean correct) {
        int count = 0;
        for (final StudentTestQuestion studentTestQuestion : getDistributedTestQuestionsSet()) {
            if (studentTestQuestion.getTestQuestionOrder().equals(order)) {
                if (correct && studentTestQuestion.getTestQuestionMark().doubleValue() >= mark) {
                    count++;
                } else if (!correct && studentTestQuestion.getTestQuestionMark() <= 0) {
                    count++;
                }
            }
        }
        return count;
    }

    public int countPartiallyCorrectAnswers(final Integer order, final double mark) {
        int count = 0;
        for (final StudentTestQuestion studentTestQuestion : getDistributedTestQuestionsSet()) {
            if (studentTestQuestion.getTestQuestionOrder().equals(order) && studentTestQuestion.getResponse() != null) {
                final double testQuestionMark = studentTestQuestion.getTestQuestionMark().doubleValue();
                if (testQuestionMark < mark && testQuestionMark > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    public int countNumberOfStudents() {
        return getDistributedTestQuestionsSet().size() / getNumberOfQuestions().intValue();
    }

    public Question findQuestionByOID(String questionId) {
        for (StudentTestQuestion studentTestQuestion : this.getDistributedTestQuestionsSet()) {
            if (studentTestQuestion.getQuestion().getExternalId().equals(questionId)) {
                return studentTestQuestion.getQuestion();
            }
        }
        return null;
    }

    public String getBeginDateTimeFormatted() {
        String result = new String();
        Calendar date = getBeginDate();
        result += date.get(Calendar.DAY_OF_MONTH);
        result += "/";
        result += date.get(Calendar.MONTH) + 1;
        result += "/";
        result += date.get(Calendar.YEAR);
        result += " ";
        date = getBeginHour();
        result += date.get(Calendar.HOUR_OF_DAY);
        result += ":";
        if (date.get(Calendar.MINUTE) < 10) {
            result += "0";
        }
        result += date.get(Calendar.MINUTE);
        return result;
    }

    public String getEndDateTimeFormatted() {
        String result = new String();
        Calendar date = getEndDate();
        result += date.get(Calendar.DAY_OF_MONTH);
        result += "/";
        result += date.get(Calendar.MONTH) + 1;
        result += "/";
        result += date.get(Calendar.YEAR);
        result += " ";
        date = getEndHour();
        result += date.get(Calendar.HOUR_OF_DAY);
        result += ":";
        if (date.get(Calendar.MINUTE) < 10) {
            result += "0";
        }
        result += date.get(Calendar.MINUTE);
        return result;
    }

    public String getBeginDayFormatted() {
        String result = new String();
        if (getBeginDate().get(Calendar.DAY_OF_MONTH) < 10) {
            result += "0";
        }
        return result.concat(Integer.valueOf(getBeginDate().get(Calendar.DAY_OF_MONTH)).toString());

    }

    public String getBeginMonthFormatted() {
        String result = new String();
        if (getBeginDate().get(Calendar.MONTH) + 1 < 10) {
            result += "0";
        }
        return result.concat(Integer.valueOf(getBeginDate().get(Calendar.MONTH) + 1).toString());
    }

    public String getBeginYearFormatted() {
        return new Integer(getBeginDate().get(Calendar.YEAR)).toString();
    }

    public String getBeginHourFormatted() {
        String result = new String();
        if (getBeginHour().get(Calendar.HOUR_OF_DAY) < 10) {
            result += "0";
        }
        return result.concat(Integer.valueOf(getBeginHour().get(Calendar.HOUR_OF_DAY)).toString());
    }

    public String getBeginMinuteFormatted() {
        String result = new String();
        if (getBeginHour().get(Calendar.MINUTE) < 10) {
            result += "0";
        }
        return result.concat(Integer.valueOf(getBeginHour().get(Calendar.MINUTE)).toString());
    }

    public String getEndDayFormatted() {
        String result = new String();
        if (getEndDate().get(Calendar.DAY_OF_MONTH) < 10) {
            result += "0";
        }
        return result.concat(Integer.valueOf(getEndDate().get(Calendar.DAY_OF_MONTH)).toString());
    }

    public String getEndMonthFormatted() {
        String result = new String();
        if (getEndDate().get(Calendar.MONTH) + 1 < 10) {
            result += "0";
        }
        return result.concat(Integer.valueOf(getEndDate().get(Calendar.MONTH) + 1).toString());
    }

    public String getEndYearFormatted() {
        return new Integer(getEndDate().get(Calendar.YEAR)).toString();
    }

    public String getEndHourFormatted() {
        String result = new String();
        if (getEndHour().get(Calendar.HOUR_OF_DAY) < 10) {
            result += "0";
        }
        return result.concat(Integer.valueOf(getEndHour().get(Calendar.HOUR_OF_DAY)).toString());

    }

    public String getEndMinuteFormatted() {
        String result = new String();
        if (getEndHour().get(Calendar.MINUTE) < 10) {
            result += "0";
        }
        return result.concat(Integer.valueOf(getEndHour().get(Calendar.MINUTE)).toString());
    }

    @Deprecated
    public java.util.Date getBeginDateDate() {
        org.joda.time.YearMonthDay ymd = getBeginDateDateYearMonthDay();
        return (ymd == null) ? null : new java.util.Date(ymd.getYear() - 1900, ymd.getMonthOfYear() - 1, ymd.getDayOfMonth());
    }

    @Deprecated
    public void setBeginDateDate(java.util.Date date) {
        if (date == null) {
            setBeginDateDateYearMonthDay(null);
        } else {
            setBeginDateDateYearMonthDay(org.joda.time.YearMonthDay.fromDateFields(date));
        }
    }

    @Deprecated
    public java.util.Date getBeginHourDate() {
        org.fenixedu.academic.util.HourMinuteSecond hms = getBeginHourDateHourMinuteSecond();
        return (hms == null) ? null : new java.util.Date(0, 0, 1, hms.getHour(), hms.getMinuteOfHour(), hms.getSecondOfMinute());
    }

    @Deprecated
    public void setBeginHourDate(java.util.Date date) {
        if (date == null) {
            setBeginHourDateHourMinuteSecond(null);
        } else {
            setBeginHourDateHourMinuteSecond(org.fenixedu.academic.util.HourMinuteSecond.fromDateFields(date));
        }
    }

    @Deprecated
    public java.util.Date getEndDateDate() {
        org.joda.time.YearMonthDay ymd = getEndDateDateYearMonthDay();
        return (ymd == null) ? null : new java.util.Date(ymd.getYear() - 1900, ymd.getMonthOfYear() - 1, ymd.getDayOfMonth());
    }

    @Deprecated
    public void setEndDateDate(java.util.Date date) {
        if (date == null) {
            setEndDateDateYearMonthDay(null);
        } else {
            setEndDateDateYearMonthDay(org.joda.time.YearMonthDay.fromDateFields(date));
        }
    }

    @Deprecated
    public java.util.Date getEndHourDate() {
        org.fenixedu.academic.util.HourMinuteSecond hms = getEndHourDateHourMinuteSecond();
        return (hms == null) ? null : new java.util.Date(0, 0, 1, hms.getHour(), hms.getMinuteOfHour(), hms.getSecondOfMinute());
    }

    @Deprecated
    public void setEndHourDate(java.util.Date date) {
        if (date == null) {
            setEndHourDateHourMinuteSecond(null);
        } else {
            setEndHourDateHourMinuteSecond(org.fenixedu.academic.util.HourMinuteSecond.fromDateFields(date));
        }
    }

    public static Map<Registration, Set<DistributedTest>> getDistributedTestsByExecutionCourse(Student student,
            ExecutionCourse executionCourse) {
        Map<Registration, Set<DistributedTest>> result = new HashMap<Registration, Set<DistributedTest>>();
        for (final Registration registration : student.getRegistrationsSet()) {
            for (StudentTestQuestion studentTestQuestion : registration.getStudentTestsQuestionsSet()) {
                if (studentTestQuestion.getDistributedTest().getTestScope().getExecutionCourse().equals(executionCourse)) {
                    Set<DistributedTest> tests = result.get(registration);
                    if (tests == null) {
                        tests = new HashSet<DistributedTest>();
                    }
                    tests.add(studentTestQuestion.getDistributedTest());
                    result.put(registration, tests);
                }
            }
        }
        return result;
    }

}
