import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Grid,
  Fab,
  Button,
  CircularProgress,
  Pagination,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import AssignmentIcon from '@mui/icons-material/Assignment';
import { useTestHistory } from '../hooks/useTests';
import { useTestMutations } from '../hooks/useTestMutations';
import TestHistoryCard from '../components/TestHistoryCard';
import TestDeleteDialog from '../components/TestDeleteDialog';
import type { TestHistoryDto } from '@/types/test';
import { PageShell, EmptyState } from '@/components/ui';

export default function TestsPage() {
  const { t } = useTranslation('test');
  const navigate = useNavigate();

  const [page, setPage] = useState(0);

  const { data, isLoading } = useTestHistory({ page, size: 12 });
  const { remove } = useTestMutations();

  const [deleteTest, setDeleteTest] = useState<TestHistoryDto | null>(null);

  const handleDelete = (test: TestHistoryDto) => {
    setDeleteTest(test);
  };

  const handleDeleteConfirm = () => {
    if (deleteTest) {
      remove.mutate(deleteTest.id, { onSuccess: () => setDeleteTest(null) });
    }
  };

  const tests = data?.content;

  return (
    <PageShell
      title={t('pageTitle')}
      subtitle={t('pageSubtitle')}
      actions={
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate('/tests/generate')}
          sx={{ display: { xs: 'none', sm: 'flex' } }}
        >
          {t('common:newTest')}
        </Button>
      }
    >
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : tests && tests.length > 0 ? (
        <>
          <Grid container spacing={2.5}>
            {tests.map((test) => (
              <Grid item xs={12} sm={6} md={4} lg={3} key={test.id}>
                <TestHistoryCard
                  test={test}
                  onDelete={handleDelete}
                />
              </Grid>
            ))}
          </Grid>
          {data && data.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
              <Pagination
                count={data.totalPages}
                page={page + 1}
                onChange={(_, p) => setPage(p - 1)}
                color="primary"
              />
            </Box>
          )}
        </>
      ) : (
        <EmptyState
          icon={<AssignmentIcon sx={{ fontSize: 'inherit' }} />}
          title={t('empty.title')}
          description={t('empty.description')}
          action={{ label: t('common:newTest'), onClick: () => navigate('/tests/generate'), icon: <AddIcon /> }}
        />
      )}

      <Fab
        color="primary"
        onClick={() => navigate('/tests/generate')}
        sx={{ position: 'fixed', bottom: 32, right: 32, display: { xs: 'flex', sm: 'none' } }}
      >
        <AddIcon />
      </Fab>

      <TestDeleteDialog
        open={Boolean(deleteTest)}
        onClose={() => setDeleteTest(null)}
        onConfirm={handleDeleteConfirm}
        test={deleteTest}
        isPending={remove.isPending}
      />
    </PageShell>
  );
}
