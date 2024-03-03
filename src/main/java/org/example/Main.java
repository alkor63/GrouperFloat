package org.example;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.example.ListService.mergingGroups;

public class Main {
    public static void main(String[] args) throws IOException {

        int numBytes = 1024 * 4;
        long start = System.nanoTime();
        String outname;
        try {
            outname = args[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            outname = "lng-4-group.txt";
            System.out.println("Результат запишем в файл по-умолчанию: lng-4-group.txt");
        }
// outname - имя файла для записи результатов
        List<List<Float>> inputList = new ArrayList<>(); //быстрый способ создать коллекцию
////        try (GZIPInputStream gzip = new GZIPInputStream(new URL("https://github.com/PeacockTeam/new-job/releases/download/v1.0/lng-4.txt.gz").openStream());
        //       URL url = new URL("https:\\github.com\\PeacockTeam\\new-job\\releases\\download\\v1.0\\lng-big.7z");
        String inputName = "C:\\Users\\alkor\\Downloads\\lng-big.7z";

        SevenZFile archiveFile = new SevenZFile(new File(inputName));
        SevenZArchiveEntry entry;
        try {
            // Go through all entries
            while ((entry = archiveFile.getNextEntry()) != null) {
                // Maybe filter by name. Name can contain a path.
                String name = entry.getName();
                if (entry.isDirectory()) {
                    System.out.println(String.format("Found directory entry %s", name));
                } else {
                    // If this is a file, we read the file content into a
                    // ByteArrayOutputStream ...
                    System.out.println(String.format("Unpacking %s ...", name));
                    ByteArrayOutputStream contentBytes = new ByteArrayOutputStream();

                    // ... using a small buffer byte array.
                    byte[] buffer = new byte[numBytes];
                    int bytesRead;
                    int cnt = 0;
                    while ((bytesRead = archiveFile.read(buffer)) != -1) {
                        contentBytes.write(buffer, 0, bytesRead);
                    }
                    String primaryContent = contentBytes.toString(StandardCharsets.UTF_8.name());
                    cnt++;
                    if (cnt % 5000 == 0)
                        System.out.println("******** content " + cnt + " ************\n " + primaryContent);

                    // Assuming the content is a UTF-8 text file we can interpret the
                    // bytes as a string.
                    List<String> contentList = Arrays.asList(primaryContent.split("\n"));
                    for (String content : contentList) {
                        List<String> scanList = Arrays.asList(content.split(";"));
// разделили строку на слова по разделителю ";"
                        try {
// преобразовываем лист слов в лист чисел
                            List<Float> result = new ArrayList<>();
                            for (String s : scanList) {
                                String x = s.replaceAll("[^\\d.]", "");
                                Float val = (x.length() > 2 ? Float.parseFloat(x) : 0);
                                result.add(val);
                            }

                            if (result.size() > 0) {
                                inputList.add(result);
                            }
// т.е. в каждой строке у нас записаны элементы - "0" либо числа типа Double
                        } catch (NumberFormatException e) {
                            System.out.println("Ошибка NumberFormat: " + e.getMessage());
                            // "бракованные" строки пропускаем
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        } finally {
            archiveFile.close();
        }

        int inputListSize = inputList.size();
        System.out.println("Размер list = " + inputListSize);
        if (inputListSize > 0) {

            inputList.sort(new MyListComparator());
            int maxL = inputList.get(0).size(); //максимальная длина строки (пока для справки)

            long beforGrupping = System.nanoTime();
            System.out.print("Подготовились к группировке за = ");
            System.out.println((beforGrupping - start) / 1_000_000 + " ms");
            System.out.println("inputListSize = " + inputListSize);
//Для последующей группировки каждый столбец собираем в Мапу(число, счетчик)
//Эти мапы (их будет inputList.size()) собираем в лист

            HashMap<Float, List<Integer>>[] groupSearchMap = new HashMap[maxL];
            for (int i = 0; i < maxL; i++) {
                Float t = inputList.get(0).get(i);
                groupSearchMap[i] = new HashMap<>(Map.of(t, new ArrayList<>()));
                //проинициализировали список пока пустыми мапами
            }
            int numDuplex = 0;
            for (int line = 0; line < inputListSize; line++) {  //идём по всему списку
                List<Float> currentList = inputList.get(line);   //текущая строка
                boolean isDuplet = false;
                for (int i = 0; (!isDuplet && i < currentList.size()); i++) {
                    Float x = currentList.get(i);                //текущий элемент строки
                    if (x > 0.0) {                               // это число
                        if (groupSearchMap[i].containsKey(x)) {//если такое число уже есть в мапе
                            List<Integer> members = groupSearchMap[i].get(x);//извлекаем список значений с номерами строк-членов группы
                            for (int j : members) {//проверяем всех членов группы
                                isDuplet = checkDuplicate(currentList, inputList.get(j));
                                // если нашли хоть одно совпадение
                                if (isDuplet) {
                                    numDuplex++;
                                    break;
                                }
                            }
                            if (!isDuplet) members.add(line);//добавляем номер строки члена группы
                            groupSearchMap[i].put(x, members); //(пере)записываем ключ и строки, в которых он находится
                        } else {
                            List<Integer> lint = new ArrayList<>();
                            lint.add(line);
                            groupSearchMap[i].put(x, lint); //записываем ключ и строку, в которой он находится
                        }
                    }
                }
            }//закончили собирать мапы
            long mapIsReady = System.nanoTime();
            System.out.print("Собрали мапы, время = ");
            System.out.println((mapIsReady - start) / 1_000_000 + " ms");
        /*
Если в мапе счетчик(длина листа) >1 ==> это группа
        далее ключи нам не нужны, только value
         пересобираем их в Set отбрасывая одиночные группы
         сравниваем сеты, начиная с конца (там они короче)
        рассортируем список компаратором (по длине сета)
        теперь у нас есть размер группы и её состав - номера строк в исходной таблице
        ==> мы готовы представить результат
         */
            List<List<Integer>>[] primaryGroups = new ArrayList[maxL];
            int[] numGroup = new int[maxL];
            int totalSum = 0;
            for (int c = 0; c < maxL; c++) {
                numGroup[c] = 0;
                primaryGroups[c] = new ArrayList<>();
                for (HashMap.Entry<Float, List<Integer>> entr : groupSearchMap[c].entrySet()) {
                    List<Integer> list = entr.getValue();
                    if (list.size() > 1) {// ==> группа
                        primaryGroups[c].add(list);
                        numGroup[c]++;
                        totalSum++;
                    }
                }
            }
            System.out.println("Частотное распределение совпадений по колонкам");
            System.out.println("numGroup = " + Arrays.toString(numGroup));
            System.out.println("Всего совпадений = " + totalSum);
            for (int c2 = maxL - 1; c2 >= 1; c2--) {
                for (int c1 = c2 - 1; c1 >= 0; c1--) {
                    int count = 0;
                    Iterator<List<Integer>> iterator = primaryGroups[c2].iterator();
                    while (iterator.hasNext()) {
                        List<Integer> listB = iterator.next();
                        boolean isDublicate = false;
                        for (int l = 0; (!isDublicate && l < primaryGroups[c1].size()); l++) {
                            List<Integer> listA = primaryGroups[c1].get(l);
                            List<Integer> tempList = mergingGroups(listA, listB);
                            if (tempList.size() > listA.size()) {
                                isDublicate = true;
                                count++;
                                primaryGroups[c1].set(l, tempList);
                                if (count % 10000 == 0) {
                                    System.out.println("column c1 = " + c1 + " c2 = " + c2 + " * Secondary group = " + count);
                                }
                            }
                        }
                        if (isDublicate) iterator.remove();
                    }
                    System.out.println("В столбцах " + c2 + " и " + c1 + " обнаружено " + count + " совпадений");
                    System.out.println("В итоге в столбце " + c2 + " осталось " + primaryGroups[c2].size() + " строк");
                }
            }
            System.out.println("Объединили lists во вторичные группы, сортируем для финального вывода");

            long resultG2 = System.nanoTime();
            System.out.print("*** время = ");
            System.out.println((resultG2 - start) / 1_000_000 + " ms");
            List<List<Integer>> resultList = new ArrayList<>();//для записи финальных групп

            System.out.println("Контрольная группировка финального столбца");
            int pGsize = primaryGroups[0].size();
            int cnt = 0;
            for (int iUp = 0; iUp < pGsize - 1; iUp++) {
                List<Integer> listL = primaryGroups[0].get(iUp);
                boolean uniqueL = true;
                for (int iDn = iUp + 1; uniqueL && iDn < pGsize; iDn++) {
                    List<Integer> listP = primaryGroups[0].get(iDn);

                    final List<Integer> tempList = mergingGroups(listP, listL);
                    if (tempList.size() > listP.size()) {//было совпадение
                        primaryGroups[0].set(iDn, tempList);
                        cnt++;
                        uniqueL = false;
//                    if (cnt % 1000 == 0) {
//                                System.out.println("column 0 Secondary dubl = " + cnt);
//                            }
                    }
                }
                if (uniqueL) resultList.add(listL);
            }
            resultList.add(primaryGroups[0].get(pGsize - 1));
            //не было в цикле, ни с кем не совпал

            System.out.println("Теперь длина списка = " + resultList.size());

            resultList.sort(new MyListSizeComparator());

            long resultIsReady = System.nanoTime();
            System.out.print("Готов представить результат, время = ");
            System.out.println((resultIsReady - start) / 1_000_000 + " ms");
            System.out.println("Записываем результат в файл ");

//Итоговый вывод
            int groupNum = resultList.size();
            int maxGroup = resultList.get(0).size();

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(outname));
                writer.write("В итоге получили " + groupNum + " неединичных групп\n");
                int g = 1;
                for (List<Integer> res : resultList) {
                    writer.newLine();
                    writer.write("Группа " + (g++));
                    for (Integer j : res) {
                        writer.newLine();
                        writer.write(
                                inputList.get(j).stream()
                                        .map(n -> n == 0 ? "\"\"" : '"' + String.valueOf(n) + '"')
                                        .collect(Collectors.joining(";")));
                    }
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
            // finish
            System.out.println("В итоге получили " + groupNum + " неединичных групп");
            System.out.println("максимальный размер группы = " + maxGroup + " строк");
            long finita = System.nanoTime();
            System.out.print("FINISH, время = ");
            System.out.println((finita - start) / 1_000_000 + " ms");

        } else {
            System.out.println("В исходном списке нет ни одной строки !!!");
        }
    }

    public static boolean checkDuplicate(List<Float> list1, List<Float> list2) {
// метод возвращает true если 2 строки одинаковые
        int listSize = list1.size();
        if (listSize != list2.size()) return false;//сначала сравниваем длину строки
//затем сравниваем сумму элементов строк
//если суммы равны, проверяем все элементы попарно
        int originNums = 0;
        for (int c = 0; c < listSize; c++) {
            if (!list1.get(c).equals(list2.get(c))) originNums++;
        }
        return (originNums <= 0);
    }
}
/*
 после первичной группировки на каждом шаге приходится сравнивать листы, соединять их и сортировать результат
 Попробую ускорить процесс, написав для него отдельный метод
 ==========================================================
 Метод mergingGroups принимает 2 листа, возвращает 1
 если ФЛАГ = true ==> return list3 else return listA
 снаружи можно будет сравнить размер листа А на входе и того, что на выходе
 */

class MyListComparator implements java.util.Comparator<List<Float>> {
    public int compare(List<Float> a, List<Float> b) {
        int res = b.size() - a.size();
        if (res != 0) return res;
        float l = b.get(0) - a.get(0);
        if (l == 0) return 0;
        else return (l > 0 ? 1 : -1);
    }
}

class MyListSizeComparator implements java.util.Comparator<List<Integer>> {
    public int compare(List<Integer> a, List<Integer> b) {
        return b.size() - a.size();
    }
}

class MySetComparator implements java.util.Comparator<Set<Integer>> {
    public int compare(Set<Integer> a, Set<Integer> b) {
        return b.size() - a.size();
    }
}
