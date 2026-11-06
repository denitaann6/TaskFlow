
import java.util.Arrays;

public class TaskSearchSort {

    // Linear Search
    public static int linearSearch(int[] ids, int key) {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == key) {
                return i;
            }
        }
        return -1;
    }

    // Binary Search (array must be sorted)
    public static int binarySearch(int[] ids, int key) {
        int low = 0;
        int high = ids.length - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (ids[mid] == key) {
                return mid;
            } else if (ids[mid] < key) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

    // Insertion Sort
    public static void insertionSort(int[] ids) {
        for (int i = 1; i < ids.length; i++) {
            int key = ids[i];
            int j = i - 1;

            while (j >= 0 && ids[j] > key) {
                ids[j + 1] = ids[j];
                j--;
            }

            ids[j + 1] = key;
        }
    }

    // Merge Sort
    public static void mergeSort(int[] ids) {
        if (ids.length <= 1) {
            return;
        }

        int mid = ids.length / 2;
        int[] left = Arrays.copyOfRange(ids, 0, mid);
        int[] right = Arrays.copyOfRange(ids, mid, ids.length);

        mergeSort(left);
        mergeSort(right);
        merge(ids, left, right);
    }

    private static void merge(int[] ids, int[] left, int[] right) {
        int i = 0, j = 0, k = 0;

        while (i < left.length && j < right.length) {
            if (left[i] <= right[j]) {
                ids[k++] = left[i++];
            } else {
                ids[k++] = right[j++];
            }
        }

        while (i < left.length) {
            ids[k++] = left[i++];
        }

        while (j < right.length) {
            ids[k++] = right[j++];
        }
    }

    // Test the algorithms
    public static void main(String[] args) {
        int[] taskIds = {105, 101, 104, 102, 103};

        System.out.println("TASKFLOW - PHASE 3");
        System.out.println("------------------");
        System.out.println("Original IDs: " + Arrays.toString(taskIds));

        System.out.println(
            "Linear Search for 104: index "
            + linearSearch(taskIds, 104)
        );

        int[] insertionIds = taskIds.clone();
        insertionSort(insertionIds);
        System.out.println(
            "Insertion Sort: " + Arrays.toString(insertionIds)
        );

        System.out.println(
            "Binary Search for 104: index "
            + binarySearch(insertionIds, 104)
        );

        int[] mergeIds = taskIds.clone();
        mergeSort(mergeIds);
        System.out.println(
            "Merge Sort: " + Arrays.toString(mergeIds)
        );
    }
}
