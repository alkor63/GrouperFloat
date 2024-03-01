package org.example;

import java.util.ArrayList;
import java.util.List;

public class ListService {
/*
после первичной группировки на каждом шаге приходится сравнивать листы, соединять их и сортировать результат
Попробую ускорить процесс, написав для него отдельный метод
==========================================================
Метод принимает 2 листа, возвращает 1
внутри используем то, что в листах цифры упорядочены
записываем всё в 3-й лист
вынимаем 1-й элемент из обоих списков, сравниваем
меньший из них записываем в лист 3 i++
если они равны - переключаем ФЛАГ в true
записываем 1 их них в лист 3, оба индекса ++
так до конца списка
если ФЛАГ = true ==> return list3 else return listA
снаружи можно будет сравнить размер листа А на входе и того, что на выходе
*/
    //listB будет поглощен, если в нем найдутся совпадения элементов с листом А
    // если совпадений нет - вернется listA,
    // если есть - отсортированная сумма двух листов без повторов
    public static List<Integer> mergingGroups(List<Integer> listA, List<Integer> listB) {
        List<Integer> mergingList = new ArrayList<>();
        int sizeA = listA.size();
        int sizeB = listB.size();
        int nB;
        int nA;
        boolean isPairs = false;
        int iA = 0;
        int iB = 0;
//        обычно listB короче
        boolean listContinue = true;
        boolean endOfListA = false;
        boolean endOfListB = false;

        while (listContinue) { // Бесконечный цикл. Все выходы через return
            if (iA >= sizeA) endOfListA = true;
            if (iB >= sizeB) endOfListB = true;
// если числа в одном из листов закончились
            if (endOfListA && !endOfListB){
                if (!isPairs) return listA;
                else {
                    for (int j = iB; j < sizeB; j++){
                        nB = listB.get(j);
                        mergingList.add(nB);
                    } // дописываем в хвост числа из 2-го листа
                    return mergingList;
                }
            }

            if (!endOfListA && endOfListB){
                if (!isPairs) return listA;
                else {
                    for (int j = iA; j < sizeA; j++){
                        nA = listA.get(j);
                        mergingList.add(nA);
                    } // дописываем в хвост числа из 2-го листа
                    return mergingList;
                }
            }
// если одновременно закончились 2 листа (совпали последние числа)
            if (endOfListA && endOfListB) {
                return (isPairs ? mergingList : listA);
            }
// если в обоих листах есть значения
            nA = listA.get(iA);
            nB = listB.get(iB);

            if (nA == nB) {
                isPairs = true;
                mergingList.add(nA);
                iA++;
                iB++;
            }
            else if (nA < nB) {
                mergingList.add(nA);
                iA++;
            }
            else {
                mergingList.add(nB);
                iB++;
            }
        }
        return (isPairs ? mergingList : listA);
    }
}
